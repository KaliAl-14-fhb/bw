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
import com.amazonaws.auth.profile.ProfileCredentialsProvider;

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
 * Die Klasse lädt die Bilder zu S3 hoch.
 * Und speichert die eingegebenen Daten des Formulars in die Datenbank -->DynamoDB
 *
 * @author khaled al-ammari
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
    static AmazonDynamoDBClient client = new AmazonDynamoDBClient(new ProfileCredentialsProvider().getCredentials());
    static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    static String tablebookslistk = "buchlistek";

    //Attribute für DynamoDB von JSP
    String titel = null;
    String autor = null;
    String jahr = null;
    String verlag = null;
    String coverbild = null;

    /**
     * Anfrageprozess für HTTP -->post
     *
     * @param request
     * @param response
     * @throws ServletException wenn Fehler beim Servlet entstehen
     * @throws IOException wenn Fehler allgemien entstehen
     * @throws FileUploadException wenn Fehler beim Upload entstehen
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, FileUploadException {

        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();

        try {
            FileItemFactory factory = new DiskFileItemFactory();
            ServletFileUpload uploader = new ServletFileUpload(factory);

            /*
             * AWS CREDENTIALS generieren
             */
            credentials = null;
            try {
                credentials = new ProfileCredentialsProvider().getCredentials();
            } catch (Exception e) {
                throw new AmazonClientException(
                        "Cannot load the credentials from the credential profiles file. " +
                                "Please make sure that your credentials file is at the correct " +
                                "location (~/.aws/credentials), and is in valid format.",  e);
            }
            AmazonS3 s3 = new AmazonS3Client(credentials);
            Region euCentral1 = Region.getRegion(Regions.EU_CENTRAL_1);
            s3.setRegion(euCentral1);
            /*
             * AWS CREDENTIALS ENDS
             */


            transferManager = new TransferManager(s3);
            bucketName = "kalis3"; //festgelegt!!!!
            //createAmazonS3Bucket(); //kein neues Bucket erstellt!!!
            /*
             * Erstellen von einer Liste und Daten erhalten
             */
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
                    System.out.println(" In != " );
                    String uploadimageName = item.getName();
                    System.out.println("Name der Datei: " + uploadimageName);
                    System.out.println("Name der Datei: " + item.toString()); // name=... StoreLocation=...

                    //wenn ein Bild nicht ausgewählt wurde, dann übergib leeren String
                    if(uploadimageName.isEmpty()){
                        uploadimageName = " ";
                    } else {
                        //ansonsten: Lade Datei hoch zu S3

                        // Hinzufügen deines lokalen Pfades oder Tomcat webapp Pfad
                        File savedFile = new File("E:\\RESEARCH\\" + item.getName());
                        savedFile.getTotalSpace();
                        item.write(savedFile);
                        PutObjectRequest reqObj = new PutObjectRequest(bucketName, item.getName(), savedFile);
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
                    // wenn ein Feld im Formular freigelassen wird,
                    // dann speichere leeren String ab
                    if(titel.isEmpty()) {
                        titel = " ";
                    } if(autor.isEmpty()) {
                        autor = " ";
                    } if(jahr.isEmpty()) {
                        jahr = " ";
                    } if(verlag.isEmpty()) {
                        verlag = " ";
                    }
                    //listMyTables();
                    saveToDynamodb(tablebookslistk, titel, autor, jahr, verlag, uploadimageName);
                    response.sendRedirect("tabelle.jsp");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            out.close();
        }
    }

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
            processRequest(request, response);
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

            processRequest(request, response);

        } catch (FileUploadException ex) {
            Logger.getLogger(S3Uploader.class.getName()).log(Level.SEVERE, null, ex);
        }
    }


    /*****************************************************
     * SPEICHERUNG AUF DYNAMODB
     *****************************************************/
    public void saveToDynamodb (String tableName, String titel, String autor, String jahr, String verlag, String coverbild) {
        System.out.println("In Methode saveToDynamodb!");
        System.out.println("Tabellenname: " + tableName);


        try {
            /*+++++++++++++++++++++++++++++++++++++++++++++
            /* Bücher in DynamoDB SCHREIBEN/HINZUFÜGEN von AWS Site Beispiele
             **********************************************/
            Map<String, AttributeValue> item = new HashMap<String, AttributeValue>();
            //item.put("id", new AttributeValue().withNS("2")); //.withN("2"));
            item.put("title", new AttributeValue().withS(titel)); // .withS("Book 101 Title"));
            item.put("autor", new AttributeValue().withS(autor)); // .withS("Book 101 Title"));
            item.put("jahr", new AttributeValue().withS(jahr)); // .withS("Book 101 Title"));
            item.put("verlag", new AttributeValue().withS(verlag)); // .withS("Book 101 Title"));
            item.put("coverbild", new AttributeValue().withS(coverbild)); // .withS("Book 101 Title"))

            PutItemRequest putItemRequest = new PutItemRequest()
                    .withTableName(tableName)
                    .withItem(item);
            PutItemResult result = client.putItem(putItemRequest);
            //item.get(new AttributeValue("title"));


            /*+++++++++++++++++++++++++++++++++++++++++++++
            /* Bücher aus DynamoDB LESEN  --->VERSCHOBEN IN DIE tabelle.jsp
             **********************************************/
            //Map<String, AttributeValue> getitem = new HashMap<String, AttributeValue>();
            //getitem.put("Id", new AttributeValue().withN(id));

            /*GetItemRequest getItemRequest = new GetItemRequest()
                    .withTableName(tableName)
                    .withKey(item);
                    //.withProjectionExpression("title, autor, jahr, verlag, coverbild");
            GetItemResult resultget = client.getItem(getItemRequest);
            System.out.println("Printing item " + resultget);
            printItem(resultget.getItem());*/

            //fetchItems(tableName);
            //getItems(tableName);

        }   catch (AmazonServiceException ase) {
            System.err.println("Failed to create item in " + tableName + " " + ase);
        }

    }

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

} //Klasse beendet