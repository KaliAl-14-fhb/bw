<html>
<head>
    <title></title>
</head>
<body>
    <h2>B&uuml;cherverwaltung</h2>

    <!-- - Die Eingabfilder müssen sich in eine Formular befinden. Die inputelemente(Eingabefelder) werden zwischen "form=formular" mit dem Attributen action gespeichert)
           FractalController.java
         - Mit <input type="submit"> definieren Sie einen Absendebutton (input = Eingabe, submit = bestätigen).
           Beim Anklicken dieses Buttons werden die Formulardaten abgeschickt, und es wird die Adresse aufgerufen,
           die im einleitenden <form>-Tag beim Attribut action angegeben ist.
           Zur unterschiedlichen weiteren Behandlung der übermittelten Daten können Sie mehrere Absendebuttons mit name-Attribut verwenden.
    -->
    <form method="post" action="S3Uploader" enctype="multipart/form-data">
        Titel: &nbsp;&nbsp;
        <input maxlength="200" name="Titel" id="Titel" type="text" value="Java" /><br />
        Autor:&nbsp;
        <input maxlength="200" name="Autor" type="text" value="Khali" /><br />
        Jahr:&nbsp;&nbsp;&nbsp;
        <input maxlength="200" name="Jahr" type="number" min="1900" max="2015" value="2014" /><br />
        Verlag:
        <input maxlength="200" name="Verlag" type="text" value="FHB" /><br />
        Cover Bild:
        <input name="DateiHochladen" id="DateiHochladen" type="file" size="50" maxlength="100000" ><br />

        <input type="submit" value="Absenden ">
    </form>
</body>
</html>
