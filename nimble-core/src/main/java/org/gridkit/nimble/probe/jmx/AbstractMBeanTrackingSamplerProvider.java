package org.gridkit.nimble.probe.jmx;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.gridkit.nimble.driver.MeteringAware;
import org.gridkit.nimble.driver.MeteringDriver;
import org.gridkit.nimble.metering.Measure;
import org.gridkit.nimble.metering.SampleFactory;
import org.gridkit.nimble.metering.SampleSchema;

public abstract class AbstractMBeanTrackingSamplerProvider implements MeteringAware<JmxAwareSamplerProvider<Runnable>>, JmxAwareSamplerProvider<Runnable>, Serializable {

	private static final long serialVersionUID = 20121023L;
	
	protected SampleSchema schema;
	protected ObjectName nameQuery;
	protected MXStruct beanProto;
	
	@Override
	public JmxAwareSamplerProvider<Runnable> attach(MeteringDriver metering) {
		schema = metering.getSchema();
		schema.freeze();
		return this;
	}
	
	protected SampleSchema configureConnectionSchema(MBeanServerConnection connection, SampleSchema root) {
		return root;
	}
	
	protected SampleSchema configureMBeanSchema(MBeanServerConnection connection, ObjectName mbeanName, SampleSchema root) {
		return root;
	}

	protected ObjectName getMBeanPatternForConnection(MBeanServerConnection connection) {
		return nameQuery;
	}

	protected MXStruct getBeanProto() {
		return beanProto;
	}
	
	protected abstract void report(SampleFactory factory, MBeanContext ctx);
	
	@Override
	public Runnable getSampler(MBeanServerConnection connection) {
		SampleSchema cs = configureConnectionSchema(connection, schema);
		cs = cs.createDerivedScheme();
		
		cs.declareDynamic(Measure.TIMESTAMP, double.class);		
		cs.declareDynamic(Measure.DURATION, double.class);
		cs.freeze();
		
		return new MBeanSampler(cs, connection, getMBeanPatternForConnection(connection));
	}
	
	private class MBeanSampler implements Runnable {

		private final MBeanServerConnection connection;
		private final ObjectName query;
		private final SampleSchema rootSchema;
		private final SampleFactory defaultFactory;
		
		private Map<String, BeanInfo> trackMap = new HashMap<String, BeanInfo>();		
		
		public MBeanSampler(SampleSchema rootSchema, MBeanServerConnection connection, ObjectName query) {
			this.rootSchema = rootSchema;
			this.defaultFactory = rootSchema.createFactory();
			this.connection = connection;
			this.query = query;
		}
		
		public synchronized void run() {
			try {
				Map<String, MXStruct> beans = JmxHelper.collectBeans(connection, query, getBeanProto());
				List<String> lost = new ArrayList<String>(trackMap.keySet());
				lost.removeAll(beans.keySet());
				for(String beanName: beans.keySet()) {
					if (trackMap.containsKey(beanName)) {
						BeanInfo bi = trackMap.get(beanName);
						bi.lastVersion = beans.get(beanName);						
						reportBean(bi);
					}
					else {
						BeanInfo bi = new BeanInfo();
						bi.mbeanName = beanName;
						
						SampleSchema ss = configureMBeanSchema(connection, new ObjectName(beanName), rootSchema);
						bi.factory = ss == rootSchema ? defaultFactory : ss.createFactory();
						bi.lastVersion = beans.get(beanName);
						reportBean(bi);
						trackMap.put(beanName, bi);
					}
				}
				for(String beanName: lost) {
					BeanInfo bi = trackMap.remove(beanName);
					bi.lastVersion = null;
					reportBean(bi);
				}
				
			} catch (Exception e) {
				// TODO logging
			} 			
		}

		private void reportBean(BeanInfo bi) {
			report(bi.factory, bi);
		}		
	}
	
	public interface MBeanContext {
		
		public String getMBeanName();		
		public MXStruct getMBeanData();
		public <V> V getUserData(String key);
		public <V> void setUserData(String key, V data);
		
	}
	
	private static class BeanInfo implements MBeanContext {
		
		String mbeanName;		
		SampleFactory factory;
		
		MXStruct lastVersion;
		Map<String, Object> userData = new HashMap<String, Object>();
		
		@Override
		public String getMBeanName() {
			return mbeanName;
		}
		@Override
		public MXStruct getMBeanData() {
			return lastVersion;
		}
		
		@Override
		@SuppressWarnings("unchecked")
		public <V> V getUserData(String key) {
			return (V) userData.get(key);
		}

		@Override
		public <V> void setUserData(String key, V data) {
			userData.put(key, data);			
		}
	}
}
