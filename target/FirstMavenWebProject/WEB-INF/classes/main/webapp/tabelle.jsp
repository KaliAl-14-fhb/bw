<%--
  Created by IntelliJ IDEA.
  User: admink
  Date: 11.12.14
  Time: 19:17
  To change this template use File | Settings | File Templates.
--%>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="com.amazonaws.services.dynamodbv2.model.*" %>
<%@ page import="com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient" %>
<%@ page import="com.amazonaws.auth.profile.ProfileCredentialsProvider" %>
<%@ page import="java.util.*" %>
<html>
<head>

</head>
<body>

<h1>Auflistung gelesener Bücher</h1>





<%
  AmazonDynamoDBClient client = new AmazonDynamoDBClient(new ProfileCredentialsProvider().getCredentials());

  HashMap<String, Condition> scanFilter = new HashMap<String, Condition>();
  Condition condition = new Condition()
          .withComparisonOperator(ComparisonOperator.GT.toString())
          .withAttributeValueList(new AttributeValue().withS("title"));
  scanFilter.put("NeuesBuch4", condition);
  ScanRequest scanRequest = new ScanRequest("buchlistek").withScanFilter(scanFilter);
  ScanResult scanResult = client.scan(scanRequest);
  System.out.println("Result: " + scanResult);
%>

<table border="1" cellpadding="1" cellspacing="1" height="91" width="687">
  <thead>
  <tr>
    <th scope="col">TITEL</th>
    <th scope="col">AUTOR</th>
    <th scope="col">JAHR</th>
    <th scope="col">VERLAG</th>
    <th scope="col">COVERBILD</th>
  </tr>
  </thead>
  <tbody>

<%
  ArrayList<Long> ids = new ArrayList<Long>();
  ArrayList<String> auts = new ArrayList<String>();

  ArrayList<AttributeValue> listValues = new ArrayList<AttributeValue>();

  ScanResult result = null;

  do{
    System.out.println("In DO-Schleife");
    ScanRequest req = new ScanRequest();
    req.setTableName("buchlistek");

    if(result != null){
      System.out.println("In if result != null");
      req.setExclusiveStartKey(result.getLastEvaluatedKey());
    }
    System.out.println("Nach IF");
    result = client.scan(req);

    System.out.println("Vor LIST");
    List<Map<String, AttributeValue>> rows = result.getItems();


    for(Map<String, AttributeValue> map : rows){

      System.out.println(listValues.toString());
      try{

        //TITEL
          AttributeValue ti = map.get("title");
          String tiString = ti.toString();

        //AUTOR
        AttributeValue au = map.get("autor");
        String auString = au.toString();

        //JAHR
        AttributeValue ja = map.get("jahr");
        String jaString = ja.toString();

        //VERLAG
        AttributeValue ver = map.get("verlag");
        String verString = ver.toString();

        //COVERBILD
        AttributeValue cov = map.get("coverbild");
        String covString = cov.toString();

        if (tiString.startsWith("{S: ") && auString.startsWith("{S: ") && jaString.startsWith("{S: ") && verString.startsWith("{S: ") && covString.startsWith("{S: ")) {
          tiString = tiString.replace("{S: ", "");
          auString = auString.replace("{S: ", "");
          jaString = jaString.replace("{S: ", "");
          verString = verString.replace("{S: ", "");
          covString = covString.replace("{S: ", "");
        }
        if (tiString.endsWith(",}") && auString.endsWith(",}") && jaString.endsWith(",}") && verString.endsWith(",}") && covString.endsWith(",}")) {
          tiString = tiString.replace(",}", "");
          auString = auString.replace(",}", "");
          jaString = jaString.replace(",}", "");
          verString = verString.replace(",}", "");
          covString = covString.replace(",}", "");
        }

                    %>
                      <tr>
                        <td>  <%= tiString.toString() %> </td>
                        <td>  <%= auString.toString() %> </td>
                        <td>  <%= jaString.toString() %> </td>
                        <td>  <%= verString.toString() %> </td>
                        <td>  <img src="http://d932q28ou71at.cloudfront.net/<%= covString.toString() %>" width="150" height="50" /> </td>
                      </tr>
                    <%

                } catch (NumberFormatException e){
                    System.out.println("In CATCH");
                    System.out.println(e.getMessage());
                }

            }
        } while(result.getLastEvaluatedKey() != null);
        System.out.println("NAch WHILE");

        /*for(AttributeValue attr: listValues) {
            System.out.println(attr);
        }*/
        //System.out.println(listValues.toString());



        //System.out.println("Result size: " + ids.size());
        /*for(String name: auts) {
            System.out.println(name);
        }*/
        //System.out.println("Autor: " + auts.get());

        System.out.println("vor ids return");
%>


<!--<table border="1" cellpadding="1" cellspacing="1" height="91" width="687">
  <thead>
  <tr>
    <th scope="col">TITEL</th>
    <th scope="col">AUTOR</th>
    <th scope="col">JAHR</th>
    <th scope="col">VERLAG</th>
    <th scope="col">COVERBILD</th>
  </tr>
  </thead>
  <tbody>
  <tr>
    <td id="title">  <%= scanResult %> </td>
    <td>&nbsp;</td>
    <td>&nbsp;</td>
    <td>&nbsp;</td>
    <td>&nbsp;</td>-->
  </tbody>
</table>





</body>
</html>
