package com.tsoft.jenkins.plugin.db;

import hudson.Extension;
import hudson.Util;
import hudson.util.FormValidation;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.interceptor.RequirePOST;
import redis.clients.jedis.Jedis;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Extension
public class RedisDatabasePoolConfiguration extends GlobalConfiguration {

    static final Logger log = Logger.getLogger(RedisDatabasePoolConfiguration.class.getName());
    private String redisServerUrl = "localhost:6379";
    private Pattern p = Pattern.compile("^([http|https]*\\:\\/\\/)?([a-zA-Z0-9\\.]+\\:\\d{2,5})$");

    /** @return the singleton instance */
    public static RedisDatabasePoolConfiguration get() {
        return GlobalConfiguration.all().get(RedisDatabasePoolConfiguration.class);
    }

    /**
     * Together with {@link #redisServerUrl}, binds to entry in {@code config.jelly}.
     * @param redisServerUrl the new value of this field
     */
    @DataBoundSetter
    public void setRedisServerUrl(String redisServerUrl) {
        this.redisServerUrl = redisServerUrl;
        save();
    }
    public String getRedisServerUrl(){ return this.redisServerUrl; }

    @RequirePOST
    public FormValidation doCheckRedisServerUrl(@QueryParameter String value) {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);
        if (Util.fixEmptyAndTrim(value) == null ) {
            return FormValidation.error("server_url can not be empty");
        }
        else {
            Matcher m = p.matcher(value);
            if( m.matches() )
                return FormValidation.ok();
            else
                return FormValidation.error("La direccion de servidor debe tener el formato: `http://server_url:port`");
        }

    }

    @RequirePOST
    public FormValidation doTestConnection(@QueryParameter String redisServerUrl) throws IOException, ServletException {
        Jenkins.get().checkPermission(Jenkins.ADMINISTER);
        if (Util.fixEmptyAndTrim(redisServerUrl) == null ) {
            return FormValidation.error("server_url can not be empty");
        }
        else {
            try {
                // Se valida el formato de server_url:port
                Matcher m = p.matcher(redisServerUrl);
                if (m.matches()) {
                    String url = m.group(2);
                    if (testConection(url)) {
                        return FormValidation.ok("Success");
                    } else {
                        return FormValidation.warning("No se puede comprobar la conexion con eL servidor.");
                    }
                } else {
                    return FormValidation.error("La direccion de servidor debe tener el formato: `http://server_url:port`");
                }
            } catch (Exception e) {
                return FormValidation.error(e.toString());
            }
        }
    }

    /**
     * Crea una conexion con el servidor de REDIS y valida la respuesta
     * @param server_url url del servidor (valor del puerto por defecto: 6397)
     */
    private boolean testConection(String server_url){
        String server = server_url.split(":")[0];
        int port = Integer.parseInt(server_url.split(":")[1]);
        String result = new Jedis(server, port).ping().toLowerCase();
        if( result.equals("pong") ) {
            return true;
        }
        else{
            log.warning("Fallo la conexion con el servidor: "+server);
            return false;
        }
    }

}
