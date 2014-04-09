package org.owasp.appsensor;

import org.owasp.appsensor.accesscontrol.AccessController;
import org.owasp.appsensor.analysis.AnalysisEngine;
import org.owasp.appsensor.configuration.server.ServerConfiguration;
import org.owasp.appsensor.configuration.server.ServerConfigurationReader;
import org.owasp.appsensor.configuration.server.StaxServerConfigurationReader;
import org.owasp.appsensor.exceptions.ConfigurationException;
import org.owasp.appsensor.listener.AttackListener;
import org.owasp.appsensor.listener.EventListener;
import org.owasp.appsensor.listener.ResponseListener;
import org.owasp.appsensor.logging.Logger;
import org.owasp.appsensor.storage.AttackStore;
import org.owasp.appsensor.storage.EventStore;
import org.owasp.appsensor.storage.ResponseStore;

/**
 * AppSensor locator class is provided to make it easy to gain access to the 
 * AppSensor classes in use. Use the set methods to override the reference 
 * implementations with instances of any custom implementations.  Alternatively, 
 * These configurations are set in the appsensor-server-config.xml file.
 * 
 * @author John Melton (jtmelton@gmail.com) http://www.jtmelton.com/
 */
public class AppSensorServer extends ObjectFactory {
	
	/** accessor for {@link org.owasp.appsensor.configuration.server.ServerConfiguration} */
	private static ServerConfiguration configuration;
	
	/** accessor for {@link org.owasp.appsensor.storage.EventStore} */
	private static EventStore eventStore;
	
	/** accessor for {@link org.owasp.appsensor.storage.AttackStore} */
	private static AttackStore attackStore;
	
	/** accessor for {@link org.owasp.appsensor.storage.ResponseStore} */
	private static ResponseStore responseStore;
	
	/** accessor for Event {@link org.owasp.appsensor.storage.AnalysisEngine} */
	private static AnalysisEngine eventAnalysisEngine;
	
	/** accessor for Attack {@link org.owasp.appsensor.storage.AnalysisEngine} */
	private static AnalysisEngine attackAnalysisEngine;
	
	/** accessor for Response {@link org.owasp.appsensor.storage.AnalysisEngine} */
	private static AnalysisEngine responseAnalysisEngine;
	
	/** accessor for {@link org.owasp.appsensor.accesscontrol.AccessController} */
	private static AccessController accessController;
	
	/**
	 * Bootstrap mechanism that loads the configuration for the server object based 
	 * on the default configuration reading mechanism. 
	 * 
	 * The reference implementation of the configuration is XML-based and a schema is 
	 * available in the appsensor_server_config_VERSION.xsd.
	 */
	public static synchronized void bootstrap() {
		bootstrap(new StaxServerConfigurationReader());
	}
	
	/**
	 * Bootstrap mechanism that loads the configuration for the server object based 
	 * on the specified configuration reading mechanism. 
	 * 
	 * The reference implementation of the configuration is XML-based, but this interface 
	 * allows for whatever mechanism is desired
	 * 
	 * @param configurationReader desired configuration reader 
	 */
	public static synchronized void bootstrap(ServerConfigurationReader configurationReader) {
		if (configuration != null) {
			throw new IllegalStateException("Bootstrapping the AppSensorServer should only occur 1 time per JVM instance.");
		}
		
		try {
			configuration = configurationReader.read();
			
			initialize();
		} catch(ConfigurationException pe) {
			throw new RuntimeException(pe);
		}
	}
	
	public static synchronized AppSensorServer getInstance() {
		if (configuration == null) {
			//if getInstance is called without the bootstrap having been run, just execute the default bootstrapping
			bootstrap();
		}
		
		return SingletonHolder.instance;
	}
	
	private static final class SingletonHolder {
		static final AppSensorServer instance = new AppSensorServer();
	}
	
	private static void initialize() {
		eventStore = null;
		attackStore = null;
		responseStore = null;
		
		//load up observer configurations on static load
		for(String observer : configuration.getEventStoreObserverImplementations()) {
			SingletonHolder.instance.getEventStore().registerListener((EventListener)make(observer, "EventStoreObserver"));
		}
		
		for(String observer : configuration.getAttackStoreObserverImplementations()) {
			SingletonHolder.instance.getAttackStore().registerListener((AttackListener)make(observer, "AttackStoreObserver"));
		}
		
		for(String observer : configuration.getResponseStoreObserverImplementations()) {
			SingletonHolder.instance.getResponseStore().registerListener((ResponseListener)make(observer, "ResponseStoreObserver"));
		}
	}
	
	//singleton
	private AppSensorServer() { }
	
	/**
	 * Accessor for ServerConfiguration object
	 * @return ServerConfiguration object
	 */
	public ServerConfiguration getConfiguration() {
		return configuration;
	}
	
	public void setConfiguration(ServerConfiguration updatedConfiguration) {
		configuration = updatedConfiguration;
	}
	
	/**
	 * Accessor for Event AnalysisEngine object
	 * @return Event AnalysisEngine object
	 */
	public AnalysisEngine getEventAnalysisEngine() {
		if (eventAnalysisEngine == null) {
			eventAnalysisEngine = make(getConfiguration().getEventAnalysisEngineImplementation(), "EventAnalysisEngine");
			eventAnalysisEngine.setExtendedConfiguration(getConfiguration().getEventAnalysisEngineExtendedConfiguration());
		}
		
		return eventAnalysisEngine;
	}
	
	/**
	 * Accessor for Attack AnalysisEngine object
	 * @return Attack AnalysisEngine object
	 */
	public AnalysisEngine getAttackAnalysisEngine() {
		if (attackAnalysisEngine == null) {
			attackAnalysisEngine = make(getConfiguration().getAttackAnalysisEngineImplementation(), "AttackAnalysisEngine");
			attackAnalysisEngine.setExtendedConfiguration(getConfiguration().getAttackAnalysisEngineExtendedConfiguration());
		}
		
		return attackAnalysisEngine;
	}
	
	/**
	 * Accessor for Response AnalysisEngine object
	 * @return Response AnalysisEngine object
	 */
	public AnalysisEngine getResponseAnalysisEngine() {
		if (responseAnalysisEngine == null) {
			responseAnalysisEngine = make(getConfiguration().getResponseAnalysisEngineImplementation(), "ResponseAnalysisEngine");
			responseAnalysisEngine.setExtendedConfiguration(getConfiguration().getResponseAnalysisEngineExtendedConfiguration());
		}
		
		return responseAnalysisEngine;
	}
	
	/**
	 * Accessor for EventStore object
	 * @return EventStore object
	 */
	public EventStore getEventStore() {
		if (eventStore == null) {
			eventStore = make(getConfiguration().getEventStoreImplementation(), "EventStore");
			eventStore.setExtendedConfiguration(getConfiguration().getEventStoreExtendedConfiguration());
		}
		
		return eventStore; 
	}
	
	/**
	 * Accessor for AttackStore object
	 * @return AttackStore object
	 */
	public AttackStore getAttackStore() {
		if (attackStore == null) {
			attackStore = make(getConfiguration().getAttackStoreImplementation(), "AttackStore");
			attackStore.setExtendedConfiguration(getConfiguration().getAttackStoreExtendedConfiguration());
		}
		
		return attackStore;
	}
	
	/**
	 * Accessor for ResponseStore object
	 * @return ResponseStore object
	 */
	public ResponseStore getResponseStore() {
		if (responseStore == null) {
			responseStore = make(getConfiguration().getResponseStoreImplementation(), "ResponseStore");
			responseStore.setExtendedConfiguration(getConfiguration().getResponseStoreExtendedConfiguration());
		}
		
		return responseStore;
	}
	
	/**
	 * Accessor for Logger object
	 * @return Logger object
	 */
	public Logger getLogger() {
		Logger logger = make(getConfiguration().getLoggerImplementation(), "Logger");
		logger.setExtendedConfiguration(getConfiguration().getLoggerExtendedConfiguration());
		
		return logger;
	}
	
	/**
	 * Accessor for AccessController object. 
	 * @return AccessController object
	 */
	public AccessController getAccessController() {
		if (accessController == null) {
			accessController = make(getConfiguration().getAccessControllerImplementation(), "AccessController");
			accessController.setExtendedConfiguration(getConfiguration().getAccessControllerExtendedConfiguration());
		}
		
		return accessController;
	}
	
}
