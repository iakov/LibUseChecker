library OkHttp;

types {
    RequestBuilder(okhttp3.Request.Builder);
    String(String);
    Request(okhttp3.Request);
    Response(okhttp3.Response);
    Client(okhttp3.OkHttpClient);
    Call(okhttp3.Call);
    ResponseBody(okhttp3.ResponseBody);
    HttpUrl(okhttp3.HttpUrl);
}

fun RequestBuilder.RequestBuilder() : RequestBuilder {
    result = new ARequestBuilder(Created);
}

fun RequestBuilder.url(urlValue: String) : RequestBuilder;

fun RequestBuilder.url(urlValue: HttpUrl) : RequestBuilder;

fun RequestBuilder.build() : Request {
    result = new ARequest(Created);
}

fun Client.Client() : Client {
    result = new AClient(Created);
}

fun Client.newCall(request: Request) : Call {
    result = new ACall(Created);
}

fun Call.execute() : Response {
    result = new AResponse(Created);
}

fun Response.body() : ResponseBody {
    result = new AResponseBody(Created);
}

fun ResponseBody.string() : String {
    post("ONE", "String-result of response body should not be empty", result != null && !result.isEmpty());
}

fun Response.close();

automaton ARequestBuilder {
    state Created, UrlSet;
    finishstate Built;

    shift Created->UrlSet (url);
    shift UrlSet->Built (build);
}

automaton ARequest {
    state Created;
}

automaton AClient {
    state Created;
}

automaton AResponse {
    state Created;
    finishstate Closed;

    shift any->Closed (close);
}

automaton AResponseBody {
    state Created;
    finishstate ResultRetrieved;

    shift any->ResultRetrieved (string);
}

automaton ACall {
    state Created;
    finishstate Executed;

    shift Created->Executed (execute);
}