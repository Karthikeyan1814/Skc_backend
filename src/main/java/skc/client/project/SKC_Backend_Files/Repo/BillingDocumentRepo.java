package skc.client.project.SKC_Backend_Files.Repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import skc.client.project.SKC_Backend_Files.entity.BillingDocument;

import java.util.List;
import java.util.Optional;

public interface BillingDocumentRepo extends JpaRepository<BillingDocument, Long> {

    Optional<BillingDocument> findByDocNumber(String docNumber);

    List<BillingDocument> findAllByOrderByCreatedAtDesc();

    @Query("SELECT b FROM BillingDocument b WHERE b.isArchived = false AND (" +
           "(:clientId IS NOT NULL AND b.clientId = :clientId) OR " +
           "(:mobile IS NOT NULL AND :mobile != '' AND b.clientMobile LIKE CONCAT('%', :mobile, '%')) OR " +
           "(:name IS NOT NULL AND :name != '' AND LOWER(b.clientName) LIKE LOWER(CONCAT('%', :name, '%')))" +
           ") ORDER BY b.createdAt DESC")
    List<BillingDocument> findForClient(
            @Param("clientId") Long clientId,
            @Param("mobile") String mobile,
            @Param("name") String name
    );
}
