package ${packageJPARepo};

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;

@Component
public interface ${newClassName}Repository extends CrudRepository<${packageName}.${entity.getPackageName()}.${newClassName}, String> {

}
