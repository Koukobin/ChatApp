class DonationHtmlPage {
  String html;
  String htmlFileName;

  DonationHtmlPage({this.html = '', this.htmlFileName = ''});

  void setHtml(String html) {
    this.html = html;
  }

  void setHtmlFileName(String htmlFileName) {
    this.htmlFileName = htmlFileName;
  }

  String getHtml() => html;

  String getHtmlFileName() => htmlFileName;

  @override
  int get hashCode => html.hashCode ^ htmlFileName.hashCode;

  @override
  bool operator ==(Object other) {
    if (identical(this, other)) return true;
    if (other is! DonationHtmlPage) return false;
    return html == other.html && htmlFileName == other.htmlFileName;
  }

  @override
  String toString() {
    return 'DonationHtmlPage {html: $html, htmlFileName: $htmlFileName}';
  }
}