package com.hanshow.fakehummer.api;

import com.hanshow.fakehummer.HummerContext;
import com.hanshow.fakehummer.api.bean.APIResponse;
import com.hanshow.fakehummer.api.bean.LogMessage;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;

@Path("/")
public class API {
    @Path("/info")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Object info() {
        HummerContext ctx = HummerContext.getInstance();
        return new APIResponse(new LinkedHashMap<String, Object>() {{
            put("address", ctx.getAddress());
            put("startTime", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(ctx.getStartTime()));
        }});
    }

    @Path("/log")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Object append(LogMessage logMessage) {
        return new APIResponse(logMessage);
    }
}
