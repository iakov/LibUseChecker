library CommonsHttpClient;

types {
    HttpClient(org.apache.commons.httpclient.HttpClient);
    DefaultHttpMethodRetryHandler(org.apache.commons.httpclient.DefaultHttpMethodRetryHandler);
    HttpClient(org.apache.commons.httpclient.HttpClient);
    HttpException(org.apache.commons.httpclient.HttpException);
    HttpStatus(org.apache.commons.httpclient.HttpStatus);
    GetMethod(org.apache.commons.httpclient.methods.GetMethod);
    HttpMethodParams(org.apache.commons.httpclient.params.HttpMethodParams);
    HttpMethodBase(org.apache.commons.httpclient.HttpMethodBase);
    HttpMethod(org.apache.commons.httpclient.HttpMethod);
    HostConfiguration(org.apache.commons.httpclient.HostConfiguration);
    HttpState(org.apache.commons.httpclient.HttpState);
    StatusLine(org.apache.commons.httpclient.StatusLine);
    String(java.lang.String);
    byte[](byte[]);
    int(int);
}

automaton AHttpClient {
    state Created;
    finishstate Used;

    shift any->Used (executeMethod);
}

automaton AGetMethod {
    state Created;
    finishstate ConnectionReleased;

    shift Created->ConnectionReleased (releaseConnection);
}

fun HttpClient.HttpClient() : HttpClient {
    result = new AHttpClient(Created);
}

fun HttpClient.executeMethod(hostConfiguration: HostConfiguration, method: HttpMethod) : int;
fun HttpClient.executeMethod(hostConfiguration: HostConfiguration, method: HttpMethod, state1: HttpState) : int;
fun HttpClient.executeMethod(method: HttpMethod) : int;

fun GetMethod.GetMethod() : GetMethod {
    result = new AGetMethod(Created);
}

fun HttpMethodBase.getParams() : HttpMethodParams;
fun HttpMethodBase.getStatusLine() : StatusLine;
fun HttpMethodBase.getResponseBody() : byte[];
fun HttpMethodBase.releaseConnection();
