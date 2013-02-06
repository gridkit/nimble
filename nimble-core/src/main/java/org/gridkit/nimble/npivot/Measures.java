package org.gridkit.nimble.npivot;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

// TODO last, first, rate, weighted rate, duration
public class Measures {
    public static boolean isMeasure(Object obj) {
        return (obj instanceof Measure<?, ?>) ||
               (obj instanceof CalculatedMeasure);
    }
    
    public static Measure<Number, Double> min(Object element) {
        return new Min(element);
    }
    
    public static Measure<Number, Double> max(Object element) {
        return new Max(element);
    }
    
    public static Measure<Number, Double> sum(Object element) {
        return new Sum(element);
    }
    
    public static CalculatedMeasure mean(Object element, Collection<? extends Object> groups) {
        return new Mean(element, groups);
    }
        
    public static CalculatedMeasure mean(Object element, Object... groups) {
        return new Mean(element, Arrays.asList(groups));
    }
    
    public static Object count(Object element) {
        return new Count(element);
    }
    
    public static CalculatedMeasure calculate(Measure<?, ?> measure, Object... groups) {
        Set<Object> sGroups = new HashSet<Object>();
        sGroups.addAll(Arrays.asList(groups));
        return calculate(measure, sGroups);
    }
    
    public static CalculatedMeasure calculate(Measure<?, ?> measure, Set<Object> groups) {
        return new CalculatedAdapter(measure, groups);
    }
   
    private static class Min extends AbstractMeasure<Number, Double> {
        private static final long serialVersionUID = -1645639397801968335L;

        public Min(Object element) {
            super(element);
        }

        @Override
        public Double addElement(Number value) {
            return value.doubleValue();
        }

        @Override
        public Double addElement(Number value, Double summary) {
            return Math.min(value.doubleValue(), summary);
        }

        @Override
        public Double addSummary(Double summary1, Double summary2) {
            return Math.min(summary1, summary2);
        }
        
        @Override
        public String toString() {
            return "Min[" + element() +"]";
        }
    }
    
    private static class Max extends AbstractMeasure<Number, Double> {
        private static final long serialVersionUID = 8122418956065510303L;

        public Max(Object element) {
            super(element);
        }

        @Override
        public Double addElement(Number value) {
            return value.doubleValue();
        }

        @Override
        public Double addElement(Number value, Double summary) {
            return Math.max(value.doubleValue(), summary);
        }

        @Override
        public Double addSummary(Double summary1, Double summary2) {
            return Math.max(summary1, summary2);
        }
        
        @Override
        public String toString() {
            return "Max[" + element() +"]";
        }
    }
    
    private static class Sum extends AbstractMeasure<Number, Double> {
        private static final long serialVersionUID = 8981969650628432552L;

        public Sum(Object element) {
            super(element);
        }

        @Override
        public Double addElement(Number value) {
            return value.doubleValue();
        }

        @Override
        public Double addElement(Number value, Double summary) {
            return value.doubleValue() + summary;
        }

        @Override
        public Double addSummary(Double summary1, Double summary2) {
            return summary1 + summary2;
        }
        
        @Override
        public String toString() {
            return "Sum[" + element() +"]";
        }
    }
    
    private static class Count extends AbstractMeasure<Object, Long> {
        private static final long serialVersionUID = 4684771857759358682L;

        public Count(Object element) {
            super(element);
        }
        
        @Override
        public Long addElement(Object value) {
            return 1l;
        }

        @Override
        public Long addElement(Object value, Long summary) {
            return summary + 1l;
        }

        @Override
        public Long addSummary(Long summary1, Long summary2) {
            return summary1 + summary2;
        }
        
        @Override
        public String toString() {
            return "Count[" + element() +"]";
        }
    }

    
    private static class Mean extends AbstractCalculatedMeasure {
        private static final long serialVersionUID = 1456457813127565881L;
        
        private final Object element;
        private final Object sum;
        private final Object count;
                
        public Mean(Object element, Collection<? extends Object> groups) {
            super(Arrays.<Object>asList(sum(element), count(element)), groups);
            this.element = element;
            this.sum = sum(element);
            this.count = count(element);
        }
        
        @Override
        public Object calculate(Sample sample) {
            Double sum = sample.get(this.sum);
            Long count = sample.get(this.count);
            return sum / count;
        }
        
        @Override
        public String toString() {            
            return Measures.toString("Mean", element, groups());
        }
    }
    
    public static String toString(String name, Object element, Set<Object> groups) {
        StringBuilder sb = new StringBuilder();
        
        sb.append(name);
        sb.append('[');
        sb.append(element);
        
        if (!groups.isEmpty()) {
            sb.append(" by ");
            sb.append(groups);
        }
        
        sb.append(']');
        
        return sb.toString();
    }
    
    public static class CalculatedAdapter extends AbstractCalculatedMeasure {
        private static final long serialVersionUID = -3236273151597056789L;
        
        private final Measure<?, ?> measure;
        
        public CalculatedAdapter(Measure<?, ?> measure, Collection<? extends Object> groups) {
            super(Collections.singleton(measure), groups);
            this.measure = measure;
        }

        @Override
        public Object calculate(Sample sample) {
            return sample.get(measure);
        }
        
        @Override
        public String toString() {
            return Measures.toString("Calculated", measure, groups());
        }
        
    }
    
    public static abstract class AbstractCalculatedMeasure implements CalculatedMeasure, Serializable {
        private static final long serialVersionUID = -6129796985377156469L;
        
        private final Set<Object> measures;
        private final Set<Object> groups;

        public AbstractCalculatedMeasure(Collection<? extends Object> measures, Collection<? extends Object> groups) {
            this.measures = new HashSet<Object>(measures);
            this.groups = new HashSet<Object>(groups);
        }

        @Override
        public Set<Object> groups() {
            return groups;
        }
        
        @Override
        public Set<Object> measures() {
            return measures;
        }
        
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result
                    + ((groups == null) ? 0 : groups.hashCode());
            result = prime * result
                    + ((measures == null) ? 0 : measures.hashCode());
            return result;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            AbstractCalculatedMeasure other = (AbstractCalculatedMeasure) obj;
            if (groups == null) {
                if (other.groups != null)
                    return false;
            } else if (!groups.equals(other.groups))
                return false;
            if (measures == null) {
                if (other.measures != null)
                    return false;
            } else if (!measures.equals(other.measures))
                return false;
            return true;
        }
    }
    
    public static abstract class AbstractMeasure<E, S> implements Measure<E, S>, Serializable {
        private static final long serialVersionUID = 1236979006493866609L;
        
        private final Object element;
        
        public AbstractMeasure(Object element) {
            this.element = element;
        }

        @Override
        public Object element() {
            return element;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result
                    + ((element == null) ? 0 : element.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            AbstractMeasure<?, ?> other = (AbstractMeasure<?, ?>)obj;
            if (element == null) {
                if (other.element != null)
                    return false;
            } else if (!element.equals(other.element))
                return false;
            return true;
        }
    }
}
