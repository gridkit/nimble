package org.gridkit.nimble.monitoring;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gridkit.nimble.orchestration.ScenarioBuilder;
import org.gridkit.nimble.orchestration.TimeLine;
import org.gridkit.nimble.pivot.Pivot;
import org.gridkit.nimble.pivot.PivotReporter;
import org.gridkit.nimble.pivot.display.PivotPrinter2;
import org.gridkit.nimble.pivot.display.PrintConfig;
import org.gridkit.nimble.print.PrettyPrinter;

public class MonitoringStack implements MonitoringBundle.ServiceProvider {
	
	private List<Bundle> bundles = new ArrayList<Bundle>();
	
	private Map<Class<?>, Provider> services = new HashMap<Class<?>, Provider>();
	private ScenarioBuilder builder;
	
	public <T> void inject(Class<T> service, final T instance) {
		services.put(service, new Provider() {
			@Override
			public Object getInstance() {
				return instance;
			}
		});
	}
	
	public <T> void declare(Class<T> service, T instance) {
		declare(service, null, instance);
	}

	public <T> void declare(Class<T> service, final String scope, final T instance) {
		services.put(service, new Provider() {
			
			ScenarioBuilder lastSb;
			T deployed;
			
			@Override
			public Object getInstance() {
				if (lastSb != builder) {
					lastSb = builder;
					deployed = scope == null ? builder.deploy(instance) : builder.deploy(scope, instance);
				}
				return deployed;
			}
		});		
	}

	public <T> void declare(Class<T> service, final LazyService<T> lazy) {
		services.put(service, new Provider() {
			
			ScenarioBuilder lastSb;
			T deployed;
			
			@Override
			public Object getInstance() {
				if (lastSb != builder) {
					lastSb = builder;
					deployed = lazy.deploy(builder, MonitoringStack.this);
				}
				return deployed;
			}
		});		
	}
	
	public void addBundle(MonitoringBundle bundle, String caption) {
		Bundle b = new Bundle(bundle, caption);
		bundles.add(b);
	}
	
	public void printSections(PrintStream ps, PivotReporter reporter) {
		for(Bundle b: bundles) {
			ps.println("\n" + b.caption + "\n");
			PivotPrinter2 pp = new PivotPrinter2();
			b.bundle.configurePrinter(pp);
			new PrettyPrinter().print(ps, pp.print(reporter.getReader()));
		}
	}
	
	public void configurePivot(Pivot pivot) {
		for(Bundle b: bundles) {
			b.bundle.configurePivot(pivot);
		}		
	}

	public void configurePrinter(PrintConfig printer) {
		for(Bundle b: bundles) {
			b.bundle.configurePrinter(printer);
		}		
	}

	public void deploy(ScenarioBuilder sb, TimeLine timeLine) {
		builder = sb;
		try {
			for(Bundle b: bundles) {
				sb.fromStart();
				b.bundle.deploy(sb, this, timeLine);
			}
		}
		finally {
			builder = null;
		}
	}
	
	@Override
	public <T> T lookup(Class<T> service) {
		Provider p = services.get(service);
		if (p == null) {
			throw new IllegalArgumentException("No instance for " + service.getName());
		}
		return service.cast(p.getInstance());
	}

	private static class Bundle {
		
		MonitoringBundle bundle;
		String caption;
		
		public Bundle(MonitoringBundle bundle, String caption) {
			this.bundle = bundle;
			this.caption = caption;
		}
	}
	
	private static interface Provider {
		
		public Object getInstance();
		
	}
	
	public static interface LazyService<T> {
		public T deploy(ScenarioBuilder sb, MonitoringBundle.ServiceProvider context);	
	}
}
