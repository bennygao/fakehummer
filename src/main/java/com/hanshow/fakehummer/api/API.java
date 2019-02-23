package com.hanshow.fakehummer.api;

import com.hanshow.fakehummer.HummerContext;
import com.hanshow.fakehummer.api.bean.APIResponse;
import com.hanshow.fakehummer.api.bean.LogMessage;
import redis.clients.jedis.Jedis;

import javax.ws.rs.*;
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
            put("environments", System.getenv());
        }});
    }

    @Path("/log")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Object append(LogMessage logMessage) {
        return new APIResponse(logMessage);
    }

    @Path("/db/{key}")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Object putKey(@PathParam("key") String key, String value) throws Exception {
        try (Jedis jedis = HummerContext.getInstance().getJedis()) {
            jedis.set(key, value);
            return new APIResponse(new LinkedHashMap<String, Object>() {{
                put("key", key);
                put("value", value);
            }});
        }
    }

    @Path("/db/{key}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Object getKey(@PathParam("key") String key) throws Exception {
        try (Jedis jedis = HummerContext.getInstance().getJedis()) {
            String value = jedis.get(key);
            return new APIResponse(new LinkedHashMap<String, Object>() {{
                put("key", key);
                put("value", value);
            }});
        }
    }
}
