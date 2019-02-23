package com.hanshow.fakehummer;

import org.apache.commons.configuration2.PropertiesConfiguration;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import static java.lang.String.format;

public class SSLConfig {
    private final static String DEFAULT_SSL_PROTOCOL = "TLSv1.2";
    private final static String DEFAULT_SSL_PROVIDER = "SunJSSE";

    private final static String DEFAULT_KEY_STORE_SEARCH_STRATEGY = "classpath";
    private final static String DEFAULT_KEY_STORE_TYPE = "JKS";
    private final static String DEFAULT_KEY_STORE_PROVIDER = "SunJSSE";
    private final static String DEFAULT_KEY_STORE_PASSWORD = "hanshow";

    private final static String DEFAULT_TRUST_STORE_TYPE = "JKS";
    private final static String DEFAULT_TRUST_STORE_PROVIDER = "SunJSSE";
    private final static String DEFAULT_TRUST_STORE_PASSWORD = "hanshow";

    private final static String DEFAULT_KEY_MANAGER_FACTORY_ALGORITHM = "SunX509";
    private final static String DEFAULT_KEY_MANAGER_FACTORY_PROVIDER = "SunJSSE";
    private final static String DEFAULT_KEY_MANAGER_FACTORY_PASSWORD = "hanshow";

    private final static String DEFAULT_TRUST_MANAGER_FACTORY_ALGORITHM = "SunX509";
    private final static String DEFAULT_TRUST_MANAGER_FACTORY_PROVIDER = "SunJSSE";
    private final static String DEFAULT_TRUST_MANAGER_FACTORY_PASSWORD = "hanshow";


    private String keyStorePath;
    private String trustStorePath;
    private PropertiesConfiguration properties;

    private KeyStore keyStore;
    private KeyManagerFactory keyManagerFactory;

    private KeyStore trustStore;
    private TrustManagerFactory trustManagerFactory;

    private SSLContext sslContext;
    private HostnameVerifier hostnameVerifier;
    // private Collection<Pattern> accessedHostPatterns;

    public SSLConfig(String keyStorePath, String trustStorePath) {
        this(keyStorePath, trustStorePath, new PropertiesConfiguration());
    }

    public SSLConfig(String keyStorePath, String trustStorePath, PropertiesConfiguration properties) {
        this.keyStorePath = keyStorePath;
        this.trustStorePath = trustStorePath;
        this.properties = properties;

        try {
            initKeyStore();
            initTrustStore();
            initSSLContext();
            initHostnameVerifier();
        } catch (Throwable t) {
            throw new RuntimeException("create Secure Sockets Layer(SSL) configuration error", t);
        }
    }

    private void initHostnameVerifier() {
        /* HostnameVerifier是在SSL客户端校验
        String[] patterns = Configuration.getInstance().getStringArray("ssl.acl");
        if (patterns == null || patterns.length == 0) {
            accessedHostPatterns = null;
            hostnameVerifier = (host, session) -> true;
        } else {
            accessedHostPatterns = Arrays.stream(patterns).map(Pattern::compile).collect(Collectors.toList());
            hostnameVerifier = (host, session) -> {
                for (Pattern pattern : accessedHostPatterns) {
                    if (pattern.matcher(host).matches()) {
                        return true;
                    }
                }

                Logger logger = LoggerFactory.getLogger("SSL");
                logger.warn("reject SSL/TLS connection from " + host);
                return false;
            };
        }
        */

        hostnameVerifier = (host, session) -> true;
    }

    private void initKeyStore()
            throws NoSuchAlgorithmException, NoSuchProviderException, KeyStoreException, UnrecoverableKeyException, IOException, CertificateException {

        keyStore = createKeyStore();
        String algorithm = getKeyManagerFactoryAlgorithm();
        String provider = getKeyManagerFactoryProvider();
        String password = getKeyManagerFactoryPassword();
        keyManagerFactory = KeyManagerFactory.getInstance(algorithm, provider);
        keyManagerFactory.init(keyStore, password.toCharArray());
    }

    private void initTrustStore()
            throws NoSuchAlgorithmException, NoSuchProviderException, KeyStoreException, IOException, CertificateException {

        if (isTrustAll()) {
            trustStore = null;
            trustManagerFactory = null;
        } else {
            trustStore = createTrustStore();
            String algorithm = getTrustManagerFactoryAlgorithm();
            String provider = getTrustManagerFactoryProvider();
            trustManagerFactory = TrustManagerFactory.getInstance(algorithm, provider);
            trustManagerFactory.init(trustStore);
        }
    }

    private void initSSLContext()
            throws NoSuchAlgorithmException, KeyManagementException {

        TrustManager[] trustManagers;
        if (isTrustAll()) {
            trustManagers = new TrustManager[]{
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }

                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                            // Trust always
                        }

                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                            // Trust always
                        }
                    }
            };
        } else {
            trustManagers = trustManagerFactory.getTrustManagers();
        }

        String protocol = getSSLProtocol();
        sslContext = SSLContext.getInstance(protocol);
        sslContext.init(keyManagerFactory.getKeyManagers(), trustManagers,
                new SecureRandom());
    }

    public HostnameVerifier getHostnameVerifier() {
        return hostnameVerifier;
    }

    public SSLContext getSSLContext() {
        return sslContext;
    }

    public SSLSocketFactory getSSLSocketFactory() {
        return sslContext.getSocketFactory();
    }

    public KeyManagerFactory getKeyManagerFactory() {
        return keyManagerFactory;
    }

    public TrustManagerFactory getTrustManagerFactory() {
        return trustManagerFactory;
    }

    public KeyStore getKeyStore() {
        return keyStore;
    }

    public KeyStore getTrustStore() {
        return trustStore;
    }

    public String getSSLProtocol() {
        return properties.getString("ssl.protocol", DEFAULT_SSL_PROTOCOL);
    }

    public String getSSLProvider() {
        return properties.getString("ssl.provider", DEFAULT_SSL_PROVIDER);
    }

    public String getKeyManagerFactoryAlgorithm() {
        String algorithm = System.getProperty("ssl.KeyManagerFactory.algorithm");
        if (algorithm == null) {
            algorithm = properties.getString("ssl.KeyManagerFactory.algorithm",
                    DEFAULT_KEY_MANAGER_FACTORY_ALGORITHM);
        }

        return algorithm;
    }

    public String getKeyManagerFactoryProvider() {
        return properties.getString("ssl.KeyManagerFactory.provider",
                DEFAULT_KEY_MANAGER_FACTORY_PROVIDER);
    }

    public String getKeyManagerFactoryPassword() {
        return properties.getString("ssl.KeyManagerFactory.password",
                DEFAULT_KEY_MANAGER_FACTORY_PASSWORD);
    }

    public String getTrustManagerFactoryAlgorithm() {
        String algorithm = System.getProperty("ssl.TrustManagerFactory.algorithm");
        if (algorithm == null) {
            algorithm = properties.getString("ssl.TrustManagerFactory.algorithm",
                    DEFAULT_TRUST_MANAGER_FACTORY_ALGORITHM);
        }

        return algorithm;
    }

    public String getTrustManagerFactoryProvider() {
        return properties.getString("ssl.TrustManagerFactory.provider",
                DEFAULT_TRUST_MANAGER_FACTORY_PROVIDER);
    }

    public String getTrustManagerFactoryPassword() {
        return properties.getString("ssl.TrustManagerFactory.password",
                DEFAULT_TRUST_MANAGER_FACTORY_PASSWORD);
    }

    private KeyStore createKeyStore()
            throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        String type = getKeyStoreType();
        String password = getKeyStorePassword();
        try (InputStream input = searchKeyStore(keyStorePath)) {
            return loadKeyStore(type, input, password);
        }
    }

    private KeyStore createTrustStore()
            throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        String type = getTrustStoreType();
        String password = getTrustStorePassword();
        try (InputStream input = searchKeyStore(trustStorePath)) {
            return loadKeyStore(type, input, password);
        }
    }

    private static KeyStore loadKeyStore(String type, InputStream ksInput, String password)
            throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        KeyStore ks = KeyStore.getInstance(type);
        ks.load(ksInput, password.toCharArray());
        return ks;
    }

    private InputStream searchKeyStore(String path) throws IOException {
        String strategy = properties.getString("ssl.keyStoreSearchStrategy", DEFAULT_KEY_STORE_SEARCH_STRATEGY);
        InputStream inputStream;
        if (DEFAULT_KEY_STORE_SEARCH_STRATEGY.equals(strategy)) {
            inputStream = getClass().getClassLoader().getResourceAsStream(path);
            if (inputStream == null) {
                throw new FileNotFoundException(format("Cannot find key store resource \"%s\" in classpath", path));
            }
        } else {
            inputStream = new FileInputStream(path);
        }

        return inputStream;
    }

    public String getKeyStoreType() {
        return properties.getString("ssl.keyStoreType", DEFAULT_KEY_STORE_TYPE);
    }

    public String getKeyStoreProvider() {
        return properties.getString("ssl.keyStoreProvider", DEFAULT_KEY_STORE_PROVIDER);

    }

    public String getKeyStorePassword() {
        return properties.getString("ssl.keyStorePassword", DEFAULT_KEY_STORE_PASSWORD);
    }

    public boolean isTrustAll() {
        return properties.getBoolean("ssl.trustAll", true);
    }

    public String getTrustStoreType() {
        return properties.getString("ssl.trustStoreType", DEFAULT_TRUST_STORE_TYPE);
    }

    public String getTrustStoreProvider() {
        return properties.getString("ssl.trustStoreProvider", DEFAULT_TRUST_STORE_PROVIDER);

    }

    public String getTrustStorePassword() {
        return properties.getString("ssl.trustStorePassword", DEFAULT_TRUST_STORE_PASSWORD);
    }
}

