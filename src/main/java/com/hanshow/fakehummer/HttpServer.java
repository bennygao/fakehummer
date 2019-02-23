package com.hanshow.fakehummer;

import java.io.File;
import java.net.URL;
import java.security.SecureRandom;

import org.apache.commons.configuration2.PropertiesConfiguration;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.server.session.DefaultSessionIdManager;
import org.eclipse.jetty.server.session.SessionDataStoreFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ScheduledExecutorScheduler;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpServer {
    private final static int DEFAULT_HTTP_PORT = 8080;
    private final static String DEFAULT_WEB_DESCRIPTOR = "web.xml";
    private final static String DEFAULT_WEB_ROOT = "./webroot";

    private PropertiesConfiguration properties;
    private SSLConfig sslConfig;
    private Server server;
    private Integer httpPort;
    private Integer httpsPort;
    private int threadsNum; // HttpServer总线程数
    private int acceptors; // 每个jetty ServerConnector中acceptor的线程数
    private int selectors; // 每个jetty ServerConnector中selector的线程数

    private File webRoot;
    private String webDescriptor;
    private String displayName;
    private String workerName;
    private SessionDataStoreFactory sessionDataStoreFactory;

    public HttpServer() {
        this(DEFAULT_WEB_DESCRIPTOR);
    }

    public HttpServer(String webDescriptor) {
        this(new File(DEFAULT_WEB_ROOT), webDescriptor, Runtime.getRuntime().availableProcessors(), new PropertiesConfiguration(), null);
    }

    public HttpServer(File webRoot, int processorsNum, PropertiesConfiguration properties, SSLConfig sslConfig) {
        this(webRoot, DEFAULT_WEB_DESCRIPTOR, processorsNum, properties, sslConfig);
    }

    public HttpServer(File webRoot, String webDescriptor, int processorsNum, PropertiesConfiguration properties, SSLConfig sslConfig) {
        Logger logger = LoggerFactory.getLogger(getClass());

        // 根据jetty缺省公式计算selectors和acceptors的值(线程数)
        selectors = Math.max(1, Math.min(4, processorsNum / 2));
        acceptors = Math.max(1, Math.min(4, processorsNum / 8));

        this.webRoot = webRoot;
        this.webDescriptor = webDescriptor;
        this.properties = properties;
        this.sslConfig = sslConfig;
        this.threadsNum = Math.max(2, processorsNum);

        httpPort = properties.getInteger("http.port", null);
        httpsPort = properties.getInteger("https.port", null);
        if (httpPort == null && httpsPort == null) {
            logger.warn("neither https.port nor httpserver.https.port is set, HTTP server listening on default port {}.", DEFAULT_HTTP_PORT);
            httpPort = DEFAULT_HTTP_PORT;
        }
        if (httpPort != null && (httpPort <= 0 || httpPort > 65535)) {
            logger.warn("Illegal http.port {}, HTTP server listening on default port {}",
                    httpPort, DEFAULT_HTTP_PORT);
            httpPort = DEFAULT_HTTP_PORT;
        }
        if (httpsPort != null && (httpsPort <= 0 || httpsPort > 65535)) {
            logger.warn("illegal httpserver.https.port {}, HTTPS service disabled.", httpsPort);
            httpsPort = null;
        }

        if (httpPort != null) {
            threadsNum += 1 + selectors + acceptors;
        }
        if (httpsPort != null) {
            threadsNum += 1 + selectors + acceptors;
        }
        this.workerName = "jetty";
        this.displayName = "JettyServer";
        this.sessionDataStoreFactory = null;
    }

    private ServerConnector createHttpConnector(int port) {
        HttpConfiguration httpConfig = new HttpConfiguration();
        httpConfig.setPersistentConnectionsEnabled(properties.getBoolean("http.server.persistentConnectionsEnabled", true));
        httpConfig.setOutputBufferSize(properties.getInt("http.server.outputBufferSize", 32768));
        httpConfig.setRequestHeaderSize(properties.getInt("http.server.requestHeaderSize", 8192));
        httpConfig.setResponseHeaderSize(properties.getInt("http.server.responseHeaderSize", 8192));
        httpConfig.setHeaderCacheSize(properties.getInt("http.server.headerCacheSize", 512));
        httpConfig.setSendServerVersion(properties.getBoolean("http.server.sendServerVersion", true));
        httpConfig.setSendDateHeader(properties.getBoolean("http.server.sendDateHeader", false));
        ServerConnector httpConnector = new ServerConnector(this.server, this.acceptors, this.selectors,
                new HttpConnectionFactory(httpConfig));
        httpConnector.setPort(port);
        httpConnector.setIdleTimeout(properties.getLong("http.server.connector.idleTimeout", 60000));
        return httpConnector;
    }

    private ServerConnector createHttpsConnector(int port) {
        HttpConfiguration httpsConfig = new HttpConfiguration();
        httpsConfig.setSecureScheme("https");
        // httpsConfig.setSecurePort(port);
        httpsConfig.setPersistentConnectionsEnabled(true);
        httpsConfig.setOutputBufferSize(32768);
        httpsConfig.addCustomizer(new SecureRequestCustomizer());

        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setProtocol(sslConfig.getSSLProtocol());
        sslContextFactory.setProvider(sslConfig.getSSLProvider());

        // 设置SSL KeyManagerFactory参数
        sslContextFactory.setKeyManagerFactoryAlgorithm(sslConfig.getKeyManagerFactoryAlgorithm());
        sslContextFactory.setKeyManagerPassword(sslConfig.getKeyManagerFactoryPassword());

        // 设置KeyStore参数
        sslContextFactory.setKeyStore(sslConfig.getKeyStore());
        sslContextFactory.setKeyStoreType(sslConfig.getKeyStoreType());
        sslContextFactory.setKeyStoreProvider(sslConfig.getKeyStoreProvider());
        sslContextFactory.setKeyStorePassword(sslConfig.getKeyStorePassword());
        sslContextFactory.setRenegotiationAllowed(false);
        sslContextFactory.setIncludeCipherSuites(
                "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256",
                "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256",
                "TLS_RSA_WITH_AES_128_CBC_SHA256",
                "TLS_RSA_WITH_AES_256_CBC_SHA256");

        // 设置TrustStore参数
        if (sslConfig.isTrustAll()) {
            sslContextFactory.setTrustAll(true);
        } else {
            sslContextFactory.setNeedClientAuth(true);
            sslContextFactory.setTrustManagerFactoryAlgorithm(sslConfig.getTrustManagerFactoryAlgorithm());
            sslContextFactory.setTrustStore(sslConfig.getTrustStore());
            sslContextFactory.setTrustStoreType(sslConfig.getTrustStoreType());
            sslContextFactory.setTrustStoreProvider(sslConfig.getTrustStoreProvider());
            sslContextFactory.setTrustStorePassword(sslConfig.getTrustStorePassword());
        }

        ServerConnector httpsConnector = new ServerConnector(this.server, this.acceptors, this.selectors,
                new SslConnectionFactory(sslContextFactory, "http/1.1"),
                new HttpConnectionFactory(httpsConfig));
        httpsConnector.setPort(port);
        httpsConnector.setIdleTimeout(60000);
        return httpsConnector;
    }

    public void start() throws Exception {
        QueuedThreadPool threadPool = new QueuedThreadPool();
        threadPool.setMaxThreads(threadsNum);
        server = new Server(threadPool);
        server.addBean(new ScheduledExecutorScheduler());
        server.addConnector(createHttpConnector(httpPort));
        if (httpsPort != null) {
            server.addConnector(createHttpsConnector(httpsPort));
        }
        if (sessionDataStoreFactory != null) {
            server.addBean(sessionDataStoreFactory);
        }

        if (workerName != null) {
            DefaultSessionIdManager sessionIdManager = new DefaultSessionIdManager(server, new SecureRandom());
            sessionIdManager.setWorkerName(workerName.replace(".", "_"));
            server.setSessionIdManager(sessionIdManager);
        }

        // 设置WEB应用环境
        String webrootPathname = webRoot.getAbsolutePath();

        // 从classpath中获得web.xml
        URL webDescriptorURL = getClass().getClassLoader().getResource(webDescriptor);
        if (webDescriptorURL == null) {
            throw new RuntimeException(String.format("Cannot find WEB descriptor file %s in classpath",
                    webDescriptor));
        }

        WebAppContext webAppContext = new WebAppContext(webrootPathname, "/");
        webAppContext.setContextPath("/");
        webAppContext.setDescriptor(webDescriptorURL.toString());
        webAppContext.setResourceBase(webrootPathname);
        webAppContext.setDisplayName(displayName);
        webAppContext.setClassLoader(Thread.currentThread().getContextClassLoader());
        webAppContext.setConfigurationDiscovered(true);
        webAppContext.setParentLoaderPriority(true);

        if (properties.getBoolean("http.server.gzip.enabled", true)) {
            GzipHandler gzipHandler = getGzipHandler();
            gzipHandler.setHandler(webAppContext);
            server.setHandler(gzipHandler);
        }

        server.start();
    }

    private GzipHandler getGzipHandler() {
        GzipHandler gzipHandler = new GzipHandler();

        String[] includedMethods = properties.getStringArray("http.server.gzip.includedMethods");
        if (includedMethods == null || includedMethods.length == 0) {
            includedMethods = new String[]{"GET", "POST", "PUT"};
        }
        gzipHandler.setIncludedMethods(includedMethods);

        String[] includedMimeTypes = properties.getStringArray("http.server.gzip.includedMimeTypes");
        if (includedMimeTypes == null || includedMimeTypes.length == 0) {
            includedMimeTypes = new String[]{"text/plain", "text/css", "text/html",
                    "application/javascript", "application/json"};
        }
        gzipHandler.setIncludedMimeTypes(includedMimeTypes);

        gzipHandler.setMinGzipSize(properties.getInt("http.server.gzip.minGzipSize", 245));
        return gzipHandler;
    }

    public void stop() throws Exception {
        server.stop();
        server.join();
    }

    public int getThreadsNum() {
        return threadsNum;
    }

    public int getAcceptors() {
        return acceptors;
    }

    public int getSelectors() {
        return selectors;
    }

    public String getWorkerName() {
        return workerName;
    }

    public void setWorkerName(String workerName) {
        this.workerName = workerName;
    }

    public void setSessionDataStoreFactory(SessionDataStoreFactory sessionDataStoreFactory) {
        this.sessionDataStoreFactory = sessionDataStoreFactory;
    }

    public SessionDataStoreFactory getSessionDataStoreFactory() {
        return sessionDataStoreFactory;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}

