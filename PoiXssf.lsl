library PoiXssf;

types {
    XSSFWorkbook(org.apache.poi.xssf.usermodel.XSSFWorkbook);
    File(java.io.File);
    InputStream(java.io.InputStream);
    OPCPackage(org.apache.poi.openxml4j.opc.OPCPackage);
    String(java.lang.String);
    XSSFSheet(org.apache.poi.xssf.usermodel.XSSFSheet);
    Iterator(java.util.Iterator);
    boolean(boolean);
    int(int);
    double(double);
    Row(org.apache.poi.ss.usermodel.Row);
    Cell(org.apache.poi.ss.usermodel.Cell);
    XSSFRow(org.apache.poi.xssf.usermodel.XSSFRow);
    XSSFCell(org.apache.poi.xssf.usermodel.XSSFCell);
}

automaton AXSSFWorkbook {
    state Created;
    finishstate Closed;

    shift any->Closed (close);
}

automaton AXSSFSheet {
    state Created;
}

automaton ARowIterator {
    state Created;
}

automaton ARow {
    state Created;
}

automaton ACell {
    state Created;
}

automaton ACellIterator {
    state Created;
}

fun XSSFWorkbook.XSSFWorkbook() : XSSFWorkbook {
    result = new AXSSFWorkbook(Created);
}

fun XSSFWorkbook.XSSFWorkbook(file: File) : XSSFWorkbook {
    result = new AXSSFWorkbook(Created);
}

fun XSSFWorkbook.XSSFWorkbook(is: InputStream) : XSSFWorkbook {
    result = new AXSSFWorkbook(Created);
}

fun XSSFWorkbook.XSSFWorkbook(pkg: OPCPackage) : XSSFWorkbook {
    result = new AXSSFWorkbook(Created);
}

fun XSSFWorkbook.XSSFWorkbook(path: String) : XSSFWorkbook {
    result = new AXSSFWorkbook(Created);
}

fun XSSFWorkbook.close();

fun XSSFWorkbook.getSheetAt(index: int) : XSSFSheet {
    result = new AXSSFSheet(Created);
}

fun XSSFWorkbook.getSheet(name: String) : XSSFSheet {
    result = new AXXSFSheet(Created);
}

fun XSSFSheet.iterator() : Iterator {
    result = new ARowIterator(Created);
}

fun Iterator.hasNext() : boolean;

fun Row.cellIterator() : Iterator {
    result = new ACellIterator(Created);
}

fun Iterator.next() : Row {
    result = new ARow(Created);
}

fun Iterator.next() : Cell {
    result = new ACell(Created);
}

fun Cell.getCellType() : int;

fun Cell.getStringCellValue() : String;

fun Cell.getNumericCellValue() : double;

fun Cell.getBooleanCellValue() : boolean;

fun XSSFWorkbook.createSheet(sheetname: String) : XSSFSheet {
    result = new AXSSFSheet(Created);
}

fun XSSFWorkbook.createSheet() : XSSFSheet {
    result = new AXSSFSheet(Created);
}

fun XSSFSheet.createRow(rownum: int) : XSSFRow {
    result = new ARow(Created);
}

fun Row.createCell(columnIndex: int) : XSSFCell {
    result = new ACell(Created);
}

fun Row.createCell(columnIndex: int, type: int) : XSSFCell {
    result = new ACell(Created);
}

fun Cell.setCellValue(value: String);

fun Cell.setCellValue(value: double);

fun Cell.setCellValue(value: boolean);
