<html>
<head>
    <title>Webseite 1</title>
</head>
<body>
    <h2>B&uuml;cherverwaltung</h2> <!-- Darstellung von Ü Buchstabe -->

    <!-- - Die erste Webseite zum speichern der Bücherdaten durch ein Formular
         - Die Cover-Bilder sollen über das Formular auf S3 gespeichert werden
    -->
    <form method="post" action="S3Uploader" enctype="multipart/form-data">

        <!-- Darstellung von Title -->
        Titel: &nbsp;&nbsp;
        <input maxlength="200" name="Titel" id="Titel" type="text" placeholder="Titel eingeben" /><br />

        <!-- Darstellung von  Autor-->
        Autor:&nbsp;
        <input maxlength="200" name="Autor" type="text" placeholder="Autor eingeben" /><br />

        <!-- Darstellung von  Jahr-->
        Jahr:&nbsp;&nbsp;&nbsp;
        <input maxlength="200" name="Jahr" type="number" min="1900" max="2015" placeholder="Jahr eingeben" /><br />

        <!-- Darstellung von  Verlag-->
        Verlag:
        <input maxlength="200" name="Verlag" type="text" placeholder="Verlag eingeben" /><br />

        <!-- Darstellung von  Cover Bild-->
        Cover Bild:
        <input name="DateiHochladen" id="DateiHochladen" type="file" size="50" maxlength="100000" ><br />

        <!-- Darstellung von  Absenden-->
        <input type="submit" value="Absenden ">
    </form>
</body>
</html>
