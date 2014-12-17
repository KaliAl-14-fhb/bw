import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.*;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

/**
 * - Die Klasse S3Uploader lädt die Bilder zu S3 hoch.
 *   Und speichert die eingegebenen Daten des Formulars in die Datenbank -->DynamoDB
 *
 * @author khaled al-ammari
 * Date: 17.12.2014
 * Abgabe Semesterprojekt Systemintegration
 */

@WebServlet(urlPatterns = {"/S3Uploader"})
public class S3Uploader extends HttpServlet {

    //Attribute (global) FÜR SPEICHERUNG AUF S3
    private static AWSCredentials credentials;
    private static TransferManager transferManager;
    private static String bucketName;
    private Upload upload;

    //Attribute (global) für SPEICHERUNG AUF DYNAMODB
    //static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    static String tablebookslistk = "buchlistek";

    //Attribute für DynamoDB von JSP
    String titel = null;
    String autor = null;
    String jahr = null;
    String verlag = null;
    String coverbild = null;



    /**
     * HTTP Handling -->Get
     *
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {

            //****** Aufruf zur UploadS3 Methode ***********
            uploadToS3(request, response);
        } catch (FileUploadException ex) {
            Logger.getLogger(S3Uploader.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * HTTP Handling -->Post
     *
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            String title = request.getParameter("Titel");
            System.out.println("Titel ist : " + title);

            //****** Aufruf zur UploadS3 Methode ***********
            uploadToS3(request, response);

        } catch (FileUploadException ex) {
            Logger.getLogger(S3Uploader.class.getName()).log(Level.SEVERE, null, ex);
        }
    }



    /**
     * Anfrageprozess (uploadToS3) für HTTP -->post
     *
     * @param request
     * @param response
     * @throws ServletException wenn Fehler beim Servlet entstehen
     * @throws IOException wenn Fehler allgemien entstehen
     * @throws FileUploadException wenn Fehler beim Upload entstehen
     */
    protected void uploadToS3(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, FileUploadException {

        //response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();

        try {
            FileItemFactory factory = new DiskFileItemFactory();
            ServletFileUpload uploader = new ServletFileUpload(factory);

                    // ************* AWS CREDENTIALS generieren   *************
                            credentials = null;
                            try {
                                credentials = new ProfileCredentialsProvider().getCredentials(); //holt credentials aus der Angelegte Datei ./aws...

                                

                            } catch (Exception e) {
                                throw new AmazonClientException(
                                        "Cannot load the credentials from the credential profiles file. " +
                                                "Please make sure that your credentials file is at the correct " +
                                                "location (~/.aws/credentials), and is in valid format.",
                                        e);
                            }
                            AmazonS3 s3 = new AmazonS3Client(credentials);
                            Region euCentral1 = Region.getRegion(Regions.EU_CENTRAL_1);
                            s3.setRegion(euCentral1);



                   // ************* AWS transferManager generieren   *************
                            transferManager = new TransferManager(s3);

                   // ************* AWS bucketName  *************
                            bucketName = "kalis3"; //festgelegt!!!!
                            //createAmazonS3Bucket(); //kein neues Bucket erstellt!!!


                   // ************* Liste erstellen und Daten erhalten vom Webseite 1   *************
                            List items = null;
                            items = uploader.parseRequest(request);
                            Iterator itr = items.iterator();
                            while (itr.hasNext()) {
                                FileItem item = (FileItem) itr.next();

                                if (item.isFormField()) {
                                    if(item.getFieldName().equals("Titel")) {
                                        titel = item.getString();
                                        System.out.println("titel: " + titel);
                                    } else if(item.getFieldName().equals("Autor")) {
                                        autor = item.getString();
                                        System.out.println("autor: " + autor);
                                    } else if(item.getFieldName().equals("Jahr")) {
                                        jahr = item.getString();
                                        System.out.println("jahr: " + jahr);
                                    } else if(item.getFieldName().equals("Verlag")) {
                                        verlag = item.getString();
                                        System.out.println("verlag: " + verlag);
                                    } else if(item.getFieldName().equals("DateiHochladen")) {
                                        coverbild = item.getString();
                                        System.out.println("coverbild: " + coverbild);
                                    }
                                }

                                if (item.getName() != null) {
                                    String uploadimageName = item.getName();
                                    System.out.println("Name der Datei: " + uploadimageName);
                                    System.out.println("Name der Datei: " + item.toString()); // name=... StoreLocation=...

                                    // ************* wenn ein Bild nicht ausgewählt wurde, dann übergib leeren String *************
                                        if(uploadimageName.isEmpty()){
                                            uploadimageName = " ";
                                        } else {
                                            //ansonsten: Lade Datei hoch zu S3


                                            uploadimageName = proofIfBildNameExists(uploadimageName);
                                            System.out.println("KOPIE uploadimageName " + uploadimageName);

                                            // Hinzufügen lokalen Pfades oder Tomcat webapp Pfad
                                            File savedFile = new File(uploadimageName);
                                            savedFile.getTotalSpace();
                                            item.write(savedFile);
                                            PutObjectRequest reqObj = new PutObjectRequest(bucketName, uploadimageName, savedFile);
                                            System.out.println("Dateiname --> " + uploadimageName);
                                            upload = transferManager.upload(reqObj);
                                            // Um Transfertstaus zu checken
                                            if (upload.isDone() == false) {
                                                //upload.wait(30000);
                                                System.out.println(" Transfer: " + upload.getDescription());
                                                System.out.println(" Zustand: " + upload.getState());
                                                System.out.println(" Progress: " + upload.getProgress().getBytesTransferred());
                                                //Thread.sleep(3000);
                                            }
                                            upload.waitForCompletion();
                                            if (upload.isDone() == false) {
                                                System.out.println(" Transfer: " + upload.getDescription());
                                                System.out.println(" Zustand: " + upload.getState());
                                                System.out.println(" Progress: " + upload.getProgress().getPercentTransferred());
                                            }
                                        }


                                   // ************* wenn ein Feld im Formular freigelassen wird, dann speichere leeren String ab *************
                                            if(titel.isEmpty()) {
                                                titel = " ";
                                            } if(autor.isEmpty()) {
                                                autor = " ";
                                            } if(jahr.isEmpty()) {
                                                jahr = " ";
                                            } if(verlag.isEmpty()) {
                                                verlag = " ";
                                            }


                                     // *****************************************************
                                     //       Zweite Schritt: Daten in DynamoDB speichern
                                     //*****************************************************
                                            // listMyTables();
                                            saveToDynamodb(tablebookslistk, titel, autor, jahr, verlag, uploadimageName);

                                    // ************* Danach weiterleitung zur Webseite 2 *************
                                            response.sendRedirect("tabelle.jsp");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            out.close();
        }
    }



    /*****************************************************
     ********** SPEICHERUNG AUF DYNAMODB *****************
     *****************************************************/
    public void saveToDynamodb (String tableName, String titel, String autor, String jahr, String verlag, String coverbild) {
        System.out.println("In Methode saveToDynamodb!");
        System.out.println("Tabellenname: " + tableName);



                                    // *************  Bücher in DynamoDB SCHREIBEN/HINZUFÜGEN von AWS Site Beispiele  ********************
                                    try {
                                        Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
                                        //item.put("id", new AttributeValue().withNS("2")); //.withN("2"));
                                        item.put("title", new AttributeValue().withS(titel)); // .withS("Book 101 Title"));
                                        item.put("autor", new AttributeValue().withS(autor)); // .withS("Book 101 Title"));
                                        item.put("jahr", new AttributeValue().withS(jahr)); // .withS("Book 101 Title"));
                                        item.put("verlag", new AttributeValue().withS(verlag)); // .withS("Book 101 Title"));
                                        item.put("coverbild", new AttributeValue().withS(coverbild)); // .withS("Book 101 Title"))


                                        // ************* AWS CREDENTIALS generieren   *************
                                                credentials = null;
                                                try {
                                                    credentials = new ProfileCredentialsProvider().getCredentials(); //holt credentials aus der Angelegte Datei ./aws...


                                                } catch (Exception e) {
                                                    throw new AmazonClientException(
                                                            "Cannot load the credentials from the credential profiles file. " +
                                                                    "Please make sure that your credentials file is at the correct " +
                                                                    "location (~/.aws/credentials), and is in valid format.",
                                                            e);
                                                }

                                              // ************* Amazon DynamoDB Client   *************
                                                AmazonDynamoDBClient client = new AmazonDynamoDBClient(credentials);

                                                PutItemRequest putItemRequest = new PutItemRequest()
                                                        .withTableName(tableName)
                                                        .withItem(item);
                                                PutItemResult result = client.putItem(putItemRequest);


                                    }   catch (AmazonServiceException ase) {
                                        System.err.println("Failed to create item in " + tableName + " " + ase);
                                    }

    }

    /*******************************************************************
     ********** Zusatz Aufgabe:
     *          Beim hochladen das gleiche Bild, wird das Bild umbenennt
     *
     ********************************************************************/
    protected String proofIfBildNameExists(String uploadimagename) {

                    // *************************************************************************
                    //               1. Die Namen der Bilder aus DynamoDB auslesen und vergleichen
                    //                  Die gleiche Funktion wie in der Tabelle
                    // *************************************************************************

                        // ************* AWS CREDENTIALS generieren   *************
                            credentials = null;
                            try {
                                credentials = new ProfileCredentialsProvider().getCredentials(); //holt credentials aus der Angelegte Datei ./aws...


                            } catch (Exception e) {
                                throw new AmazonClientException(
                                        "Cannot load the credentials from the credential profiles file. " +
                                                "Please make sure that your credentials file is at the correct " +
                                                "location (~/.aws/credentials), and is in valid format.",
                                        e);
                            }

                    // ************* Amazon DynamoDB Client   *************
                        AmazonDynamoDBClient client = new AmazonDynamoDBClient(credentials);

                        HashMap<String, Condition> scanFilter = new HashMap<String, Condition>();
                        Condition condition = new Condition()
                                .withComparisonOperator(ComparisonOperator.GT.toString())
                                .withAttributeValueList(new AttributeValue().withS("title"));
                        scanFilter.put("NeuesBuch4", condition);
                        ScanRequest scanRequest = new ScanRequest("buchlistek").withScanFilter(scanFilter);
                        ScanResult scanResult = client.scan(scanRequest);
                        System.out.println("Result: " + scanResult);

                        ArrayList<Long> ids = new ArrayList<Long>();
                        ArrayList<String> auts = new ArrayList<String>();

                        ArrayList<AttributeValue> listValues = new ArrayList<AttributeValue>();

                        ScanResult result = null;

                        do{
                            ScanRequest req = new ScanRequest();
                            req.setTableName("buchlistek");

                            if(result != null){
                                System.out.println("In if result != null");
                                req.setExclusiveStartKey(result.getLastEvaluatedKey());
                            }
                            result = client.scan(req);
                            List<Map<String, AttributeValue>> rows = result.getItems();

                            for(Map<String, AttributeValue> map : rows){
                                System.out.println(listValues.toString());
                                try{
                                    //COVERBILD
                                    AttributeValue cov = map.get("coverbild");
                                    String covString = cov.toString();

                                    if (covString.startsWith("{S: ")) {
                                        covString = covString.replace("{S: ", "");
                                    }
                                    if (covString.endsWith(",}")) {
                                        covString = covString.replace(",}", "");
                                    }

                                    System.out.println("covString.toString()" + covString.toString());


                                    // ***********************************************************************************
                                    //  2. Wenn die Namen der Bilder gleich sind, dann benenne es um (_Kopie) in DynamoDB
                                    // ***********************************************************************************
                                                    if(uploadimagename.equals(covString)) {
                                                        uploadimagename = uploadimagename + "_Kopie";
                                                        System.out.println(uploadimagename);
                                                        return uploadimagename;
                                                    }

                                }   catch (NumberFormatException e){
                                        System.out.println(e.getMessage());
                                    }

                            }
                        } while(result.getLastEvaluatedKey() != null);
                            return uploadimagename;
    }
} //Klasse beendet






/*+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
/* Ab hier zum TESTEN gewesen für mich
*******************************************************************/
    /*static void listMyTables() {
        String lastEvaluatedTableName = null;
        do {
            ListTablesRequest listTablesRequest = new ListTablesRequest()
                    .withLimit(10)
                    .withExclusiveStartTableName(lastEvaluatedTableName);

            ListTablesResult result = client.listTables(listTablesRequest);
            lastEvaluatedTableName = result.getLastEvaluatedTableName();

            for (String name : result.getTableNames()) {
                System.out.println(name);
            }

        } while (lastEvaluatedTableName != null);
    }*/

    /*private static void printItem(Map<String, AttributeValue> attributeList) {
        for (Map.Entry<String, AttributeValue> item : attributeList.entrySet()) {
            String attributeName = item.getKey();
            AttributeValue value = item.getValue();
            System.out.println(attributeName + " "
                    + (value.getS() == null ? "" : "S=[" + value.getS() + "]")
                    + (value.getN() == null ? "" : "N=[" + value.getN() + "]")
                    + (value.getB() == null ? "" : "B=[" + value.getB() + "]")
                    + (value.getSS() == null ? "" : "SS=[" + value.getSS() + "]")
                    + (value.getNS() == null ? "" : "NS=[" + value.getNS() + "]")
                    + (value.getBS() == null ? "" : "BS=[" + value.getBS() + "] \n"));
        }
    }*/

    //Quelle: http://deveshsharma.info/2013/08/22/how-to-fetch-all-items-from-a-dynamodb-table-in-java/
    /*private static ArrayList<Long> fetchItems(String tableName) {
        ArrayList<Long> ids = new ArrayList<Long>();
        ArrayList<String> auts = new ArrayList<String>();

        ArrayList<AttributeValue> listValues = new ArrayList<AttributeValue>();

        ScanResult result = null;

        do{
            System.out.println("In DO-Schleife");
            ScanRequest req = new ScanRequest();
            req.setTableName(tableName);

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
                    System.out.println("In TRY");
                    /*AttributeValue v = map.get("title");
                    String id = v.getS();
                    ids.add(Long.parseLong(id));*/

                    /*AttributeValue ti = map.get("title");
                    //String tit = ti.getS();
                    //tit.add(tit);

                    AttributeValue au = map.get("autor");
                    String aut = au.getS();
                    auts.add(aut);

                    listValues.add(au);
                    listValues.add(ti);


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

        /*System.out.println("vor ids return");
        return ids;
    }*/


    //Quelle: https://github.com/amazonwebservices/aws-sdk-for-java/blob/master/src/samples/AmazonDynamoDB/AmazonDynamoDBSample.java
    /*static void getItems(String tableName) {
        // Scan items for movies with a year attribute greater than 1985
        HashMap<String, Condition> scanFilter = new HashMap<String, Condition>();
        Condition condition = new Condition()
                .withComparisonOperator(ComparisonOperator.GT.toString())
                .withAttributeValueList(new AttributeValue().withS("title"));
        //scanFilter.put("title", condition);
        ScanRequest scanRequest = new ScanRequest(tableName).withScanFilter(scanFilter);
        ScanResult scanResult = client.scan(scanRequest);
        System.out.println("Result: " + scanResult);
    }



    protected void getItems2(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html");
        request.setAttribute("todo", "10");
        request.getRequestDispatcher("/tabelle.jsp").forward(request, response);
    }


    /* nicht mehr gebraucht, da ein Bucket bereits existiert
    private void createAmazonS3Bucket() {
        try {
            if (transferManager.getAmazonS3Client().doesBucketExist(bucketName) == false) {
                transferManager.getAmazonS3Client().createBucket(bucketName);
            }
        } catch (AmazonClientException ace) {
            ace.printStackTrace();
        }
    }*/
    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">

   /* private static void createTable(String tableName, long readCapacityUnits, long writeCapacityUnits,
                                    String hashKeyName, String hashKeyType) {

        createTable(tableName, readCapacityUnits, writeCapacityUnits, hashKeyName,  hashKeyType, null, null);
    }

    private static void createTable(String tableName, long readCapacityUnits, long writeCapacityUnits,
                                    String hashKeyName, String hashKeyType, String rangeKeyName, String rangeKeyType) {

        try {
            System.out.println("Erzeugte Tabelle: " + tableName);
            ArrayList<KeySchemaElement> keySchemaElements = new ArrayList<KeySchemaElement>();
            ArrayList<AttributeDefinition> attributeDefinitions = new ArrayList<AttributeDefinition>();

            keySchemaElements.add(new KeySchemaElement()
                    .withAttributeName(hashKeyName)
                    .withKeyType(KeyType.HASH));
            attributeDefinitions.add(new AttributeDefinition()
                    .withAttributeName(hashKeyName)
                    .withAttributeType(hashKeyType));

            attributeDefinitions.add(new AttributeDefinition()
                    .withAttributeName("AlbumTitle")
                    .withAttributeType("S"));

            if (rangeKeyName != null) {
                keySchemaElements.add(new KeySchemaElement()
                        .withAttributeName(rangeKeyName)
                        .withKeyType(KeyType.RANGE));
                attributeDefinitions.add(new AttributeDefinition()
                        .withAttributeName(rangeKeyName)
                        .withAttributeType(rangeKeyType));
            }

            // Provide initial provisioned throughput values as Java long data types
            ProvisionedThroughput provisionedthroughput = new ProvisionedThroughput()
                    .withReadCapacityUnits(readCapacityUnits)
                    .withWriteCapacityUnits(writeCapacityUnits);

            CreateTableRequest request = new CreateTableRequest()
                    .withTableName(tableName)
                    .withKeySchema(keySchemaElements)
                    .withProvisionedThroughput(provisionedthroughput);

            // If this is the Reply table, define a local secondary index
            /*if (tableName.equals(tableName)) {
                attributeDefinitions.add(new AttributeDefinition().withAttributeName("PostedBy").withAttributeType("S"));

                ArrayList<KeySchemaElement> iks = new ArrayList<KeySchemaElement>();
                iks.add(new KeySchemaElement().withAttributeName(
                        hashKeyName).withKeyType(KeyType.HASH));
                iks.add(new KeySchemaElement().withAttributeName(
                        "PostedBy").withKeyType(KeyType.RANGE));

                LocalSecondaryIndex lsi = new LocalSecondaryIndex().withIndexName("PostedBy-Index")
                        .withKeySchema(iks)
                        .withProjection(new Projection().withProjectionType(ProjectionType.KEYS_ONLY));

                ArrayList<LocalSecondaryIndex> localSecondaryIndexes = new ArrayList<LocalSecondaryIndex>();
                localSecondaryIndexes.add(lsi);

                request.setLocalSecondaryIndexes(localSecondaryIndexes);
            }*/

            /*request.setAttributeDefinitions(attributeDefinitions);

            client.createTable(request);

        } catch (AmazonServiceException ase) {
            System.err.println("Failed to create table " + tableName + " " + ase);
        }
    }*/

