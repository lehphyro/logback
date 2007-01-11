package ch.qos.logback.classic.net;

import java.util.Properties;

import javax.jms.ObjectMessage;
import javax.naming.Context;

import junit.framework.TestCase;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.net.mock.MockInitialContext;
import ch.qos.logback.classic.net.mock.MockInitialContextFactory;
import ch.qos.logback.classic.net.mock.MockTopic;
import ch.qos.logback.classic.net.mock.MockTopicConnectionFactory;
import ch.qos.logback.classic.net.mock.MockTopicPublisher;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.ContextBase;

public class JMSTopicAppenderTest extends TestCase {

  ch.qos.logback.core.Context context;
  JMSTopicAppender appender;

  @Override
  protected void setUp() throws Exception {
    context = new ContextBase();
    appender = new JMSTopicAppender();
    appender.setContext(context);
    appender.setName("jmsTopic");
    appender.tcfBindingName = "topicCnxFactory";
    appender.topicBindingName = "testTopic";
    appender.providerURL = "url";
    appender.initialContextFactoryName = MockInitialContextFactory.class.getName();
    
    MockInitialContext mic = MockInitialContextFactory.getContext();
    mic.map.put(appender.tcfBindingName, new MockTopicConnectionFactory());
    mic.map.put(appender.topicBindingName, new MockTopic(appender.topicBindingName));
    
    super.setUp();
  }

  @Override
  protected void tearDown() throws Exception {
    appender = null;
    context = null;
    super.tearDown();
  }

  public void testAppendOk() { 
    appender.start();

    LoggingEvent le = createLoggingEvent();
    appender.append(le);
    
    MockTopicPublisher tp = (MockTopicPublisher)appender.topicPublisher;
    assertEquals(1, tp.getMessageList().size());
    ObjectMessage message = (ObjectMessage) tp.getMessageList().get(0);
    try {
      assertEquals(le, message.getObject());
    } catch (Exception e) {
      fail();
    }
  }

  public void testAppendFailure() {
    appender.start();
    
    //make sure the append method does not work
    appender.topicPublisher = null;
    
    LoggingEvent le = createLoggingEvent();
    for (int i = 1; i <= 3; i++) {
      appender.append(le);
      assertEquals(i, context.getStatusManager().getCount());
      assertTrue(appender.isStarted());
    }
    appender.append(le);
    assertEquals(4, context.getStatusManager().getCount());
    assertFalse(appender.isStarted());
  }

  public void testBuildEnvProperties() {
    appender.initialContextFactoryName = "icfn";
    appender.providerURL = "url";
    appender.urlPkgPrefixes = "pkgPref";
    appender.securityPrincipalName = "user";
    appender.securityCredentials = "cred";

    Properties props = appender.buildEnvProperties();
    assertEquals(5, props.size());
    assertEquals(appender.initialContextFactoryName, props
        .getProperty(Context.INITIAL_CONTEXT_FACTORY));
    assertEquals(appender.providerURL, props.getProperty(Context.PROVIDER_URL));
    assertEquals(appender.urlPkgPrefixes, props
        .getProperty(Context.URL_PKG_PREFIXES));
    assertEquals(appender.securityPrincipalName, props
        .getProperty(Context.SECURITY_PRINCIPAL));
    assertEquals(appender.securityCredentials, props
        .getProperty(Context.SECURITY_CREDENTIALS));
  }

  public void testBuildEnvPropertiesWithNullProviderURL() {
    appender.initialContextFactoryName = "icfn";
    appender.providerURL = null;
    appender.urlPkgPrefixes = "pkgPref";
    appender.securityPrincipalName = "user";
    appender.securityCredentials = "cred";

    Properties props = appender.buildEnvProperties();
    assertEquals(4, props.size());
    assertEquals(appender.initialContextFactoryName, props
        .getProperty(Context.INITIAL_CONTEXT_FACTORY));
    assertEquals(null, props.getProperty(Context.PROVIDER_URL));
    assertEquals(appender.urlPkgPrefixes, props
        .getProperty(Context.URL_PKG_PREFIXES));
    assertEquals(appender.securityPrincipalName, props
        .getProperty(Context.SECURITY_PRINCIPAL));
    assertEquals(appender.securityCredentials, props
        .getProperty(Context.SECURITY_CREDENTIALS));

    assertEquals(1, context.getStatusManager().getCount());
  }

  public void testBuildEnvPropertiesWithNullCredentials() {
    appender.initialContextFactoryName = "icfn";
    appender.providerURL = "url";
    appender.urlPkgPrefixes = "pkgPref";
    appender.securityPrincipalName = "user";
    appender.securityCredentials = null;

    Properties props = appender.buildEnvProperties();
    assertEquals(4, props.size());
    assertEquals(appender.initialContextFactoryName, props
        .getProperty(Context.INITIAL_CONTEXT_FACTORY));
    assertEquals(appender.providerURL, props.getProperty(Context.PROVIDER_URL));
    assertEquals(appender.urlPkgPrefixes, props
        .getProperty(Context.URL_PKG_PREFIXES));
    assertEquals(appender.securityPrincipalName, props
        .getProperty(Context.SECURITY_PRINCIPAL));
    assertEquals(null, props
        .getProperty(Context.SECURITY_CREDENTIALS));

    assertEquals(1, context.getStatusManager().getCount());
  }
  
  public void testBuildEnvPropertiesWithPkgNull() {
    appender.initialContextFactoryName = "icfn";
    appender.providerURL = "url";
    appender.urlPkgPrefixes = null;
    appender.securityPrincipalName = "user";
    appender.securityCredentials = "cred";

    Properties props = appender.buildEnvProperties();
    assertEquals(4, props.size());
    assertEquals(appender.initialContextFactoryName, props
        .getProperty(Context.INITIAL_CONTEXT_FACTORY));
    assertEquals(appender.providerURL, props.getProperty(Context.PROVIDER_URL));
    assertEquals(null, props
        .getProperty(Context.URL_PKG_PREFIXES));
    assertEquals(appender.securityPrincipalName, props
        .getProperty(Context.SECURITY_PRINCIPAL));
    assertEquals(appender.securityCredentials, props
        .getProperty(Context.SECURITY_CREDENTIALS));

    assertEquals(0, context.getStatusManager().getCount());
  }

  public void testStartMinimalInfo() {
    //let's leave only what's in the setup()
    //method, minus the providerURL
    appender.providerURL = null;
    appender.start();
    
    assertTrue(appender.isStarted());
    
    try {
      assertEquals(appender.topicBindingName, appender.topicPublisher.getTopic().getTopicName());
    } catch (Exception e) {
      fail();
    }
  }
  
  public void testStartUserPass() {
    appender.userName = "";
    appender.password = "";
    
    appender.start();
    
    assertTrue(appender.isStarted());
    
    try {
      assertEquals(appender.topicBindingName, appender.topicPublisher.getTopic().getTopicName());
    } catch (Exception e) {
      fail();
    }
  }
  
  public void testStartFails() {
    appender.topicBindingName = null;
    
    appender.start();
    
    assertFalse(appender.isStarted());
  }

  private LoggingEvent createLoggingEvent() {
    LoggingEvent le = new LoggingEvent();
    le.setLevel(Level.DEBUG);
    le.setMessage("test message");
    le.setTimeStamp(System.currentTimeMillis());
    le.setThreadName(Thread.currentThread().getName());
    return le;
  }
}