package ${entity.packageName};

import com.etendoerp.sequences.services.NonTransactionalSequenceServiceImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.MappingException;
import org.hibernate.boot.spi.InFlightMetadataCollector;
import org.hibernate.boot.spi.MetadataContributor;
import org.jboss.jandex.IndexView;
import com.etendoerp.sequences.DefaultSequenceGenerator;

/**
 * (This class is generated automatically)
 * A Metadata Contributor to allow inserting annotations that are not currently supported via the Hibernate XML mapping.
 * Classes that implement this contributor should add any annotation extending {@link com.etendoerp.sequences.DefaultSequenceGenerator}
 * to the properties which map to a column that has a Sequence reference.
 */
public class ${entity.simpleClassName}SequenceContributor implements MetadataContributor {
    private static final Logger log = LogManager.getLogger();

    @Override
    public void contribute(InFlightMetadataCollector inFlightMetadataCollector, IndexView indexView) {
        var entity = inFlightMetadataCollector.getEntityBindingMap().get("${entity.name}");
        DefaultSequenceGenerator generator = null;
        if (entity != null) {
            String sequencedProperty = null;
            try {
                <#list entity.sequencedColumnProperties as p>
                // Add sequence number generator to property ${p.name}
                sequencedProperty = "${p.name}";
                generator = new ${p.getSequenceGeneratorClassName()}(sequencedProperty);
                entity.getProperty(sequencedProperty).setValueGenerationStrategy(generator);

                <#if p.DBSequenceName?has_content>
                // Register the non transactional db sequence
                if (NonTransactionalSequenceServiceImpl.INSTANCE.getDatabase() == null) {
                    NonTransactionalSequenceServiceImpl.INSTANCE.setDatabase(inFlightMetadataCollector.getDatabase());
                }
                NonTransactionalSequenceServiceImpl.INSTANCE.registerGenerator("${p.DBSequenceName}", "${p.DBSequenceInitialValue}", "${p.DBSequenceIncrementValue}");
                </#if>

                </#list>
            } catch (MappingException e) {
                log.warn("Trying to set a generation strategy to property {} but it does not exists on entity {}. Ignoring.", sequencedProperty, "${entity.name}");
            }
        }
    }
}
