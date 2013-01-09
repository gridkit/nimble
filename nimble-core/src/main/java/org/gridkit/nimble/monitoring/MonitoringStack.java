package org.gridkit.nimble.monitoring;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gridkit.nimble.orchestration.ScenarioBuilder;
import org.gridkit.nimble.orchestration.TimeLine;
import org.gridkit.nimble.pivot.Pivot;
import org.gridkit.nimble.pivot.PivotReporter;
import org.gridkit.nimble.pivot.display.PivotPrinter2;
import org.gridkit.nimble.pivot.display.PrintConfig;
import org.gridkit.nimble.print.CsvPrinter;
import org.gridkit.nimble.print.HtmlPrinter;
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

	public void printSections(PrintStream ps, PivotReporter reporter) {
		for(Bundle b: bundles) {
			ps.println("\n" + b.caption + "\n");
			PivotPrinter2 pp = new PivotPrinter2();
			b.bundle.configurePrinter(pp);
			new PrettyPrinter().print(ps, pp.print(reporter.getReader()));
		}
	}
	
    public void printSections(PrintStream ps, PivotReporter reporter, HtmlPrinter printer) {
        for(Bundle b: bundles) {
            PivotPrinter2 pp = new PivotPrinter2();
            b.bundle.configurePrinter(pp);
            printer.setCaption(b.caption);
            printer.print(ps, pp.print(reporter.getReader()));
        }
    }

	public void reportToCsv(String fileName, PivotReporter reporter) throws IOException {
		reportToCsv(fileName, reporter, new PivotPrinter2());
	}

	public void reportToCsv(String fileName, PivotReporter reporter, PivotPrinter2 pp) throws IOException {
		CsvPrinter csv = new CsvPrinter();
		csv.setPrintHead(true);
		if (fileName.startsWith("~/")) {
			fileName = new File(new File(System.getProperty("user.home")), fileName.substring(2)).getCanonicalPath();
		}
		
		for(Bundle b: bundles) {
			b.bundle.configurePrinter(pp);
		}
		
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		PrintStream stream = new PrintStream(bos);
		csv.print(stream, pp.print(reporter.getReader()));
		stream.close();
		String[] lines = new String(bos.toByteArray()).split("[\r]?[\n]");
				
		String header = lines[0];
		rotateCsvFile(fileName, header);
		
		boolean exists = new File(fileName).exists();
		FileOutputStream fos = new FileOutputStream(fileName, true);
		PrintStream ps = new PrintStream(fos);
		for(int i = exists ? 1 : 0; i != lines.length; ++i) {
			ps.println(lines[i]);
		}
		ps.close();
	}

	private void rotateCsvFile(String fileName, String newHeader) throws IOException {
		File f = new File(fileName);
		if (f.exists()) {
			BufferedReader br = new BufferedReader(new FileReader(f));
			String header = br.readLine();
			if (!newHeader.equals(header)) {
				String newName = fileName + "." + new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
				copy(fileName, newName);
				FileOutputStream fos = new FileOutputStream(f, false);
				PrintStream ps = new PrintStream(fos);
				ps.println(newHeader);
				ps.close();
			}
		}		
	}
	
	private void copy(String oldFile, String newFile) throws IOException {
		FileInputStream fis = new FileInputStream(oldFile);
		FileOutputStream fos = new FileOutputStream(newFile);
		byte[] buf = new byte[16 << 10];
		while(true) {
			int n = fis.read(buf);
			if (n == -1) {
				break;
			}
			fos.write(buf,0,n);
		}
		fis.close();
		fos.close();
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
