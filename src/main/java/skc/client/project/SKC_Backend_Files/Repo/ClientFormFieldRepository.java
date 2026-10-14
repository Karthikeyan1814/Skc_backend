
package skc.client.project.SKC_Backend_Files.Repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import skc.client.project.SKC_Backend_Files.entity.ClientFormField;

public interface ClientFormFieldRepository
        extends JpaRepository<ClientFormField, Long> {

    // Get distinct form versions
    @Query("""
        SELECT DISTINCT f.formversion
        FROM ClientFormField f
        WHERE f.formversion IS NOT NULL
        ORDER BY f.formversion
    """)
    List<Integer> findDistinctFormVersions();

    // Search labels in a specific form version
    List<ClientFormField>
    findByFormversionAndFieldLabelContainingIgnoreCase(
            Integer formversion,
            String fieldLabel
    );

    // Get fields for one form version
    List<ClientFormField> findByFormversion(Integer formversion);

    // Get active fields for one form version
    List<ClientFormField> findByFormversionAndActiveTrue(
            Integer formversion
    );

    // Check whether a field key already exists in a version
    boolean existsByFormversionAndFieldKeyIgnoreCase(
            Integer formversion,
            String fieldKey
    );

    // Check whether a field label already exists in a version
    boolean existsByFormversionAndFieldLabelIgnoreCase(
            Integer formversion,
            String fieldLabel
    );
    
}
