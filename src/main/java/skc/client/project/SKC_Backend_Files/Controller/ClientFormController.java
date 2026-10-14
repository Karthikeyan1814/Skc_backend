package skc.client.project.SKC_Backend_Files.Controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import skc.client.project.SKC_Backend_Files.Repo.ClientFormFieldRepository;
import skc.client.project.SKC_Backend_Files.dto.ClientFormFieldRequest;
import skc.client.project.SKC_Backend_Files.entity.ClientFormField;

@RestController
@RequestMapping("/skc/Admin/ClientForm")
public class ClientFormController {

	private final ClientFormFieldRepository repository;

	public ClientFormController(ClientFormFieldRepository repository) {
		this.repository = repository;
	}

	@PostMapping("/Field")
	public ResponseEntity<?> createField(@RequestBody ClientFormFieldRequest request) {

		ClientFormField field = new ClientFormField();

		field.setFieldLabel(request.getFieldLabel());
		field.setFieldType(request.getFieldType());
		field.setPlaceholder(request.getPlaceholder());
		field.setRequired(request.isRequired());
		field.setActive(request.isActive());
		field.setFormversion(request.getFormversion());
		field.setOptions(request.getOptions());
		Integer version = request.getFormversion();
		String fieldKey = request.getFieldKey().trim();

		if (repository.existsByFormversionAndFieldKeyIgnoreCase(version, fieldKey)) {
			return ResponseEntity.status(HttpStatus.CONFLICT)
					.body(Map.of("message", "This field key already exists in form version " + version));
		}

		field.setFieldKey(fieldKey);
		field.setFormversion(version);

		ClientFormField saved = repository.save(field);
		return ResponseEntity.ok(saved);

	}

	@GetMapping("/Fields")
	public ResponseEntity<List<ClientFormField>> getFields(@RequestParam("formversion") Integer version) {

		return ResponseEntity.ok(repository.findByFormversionAndActiveTrue(version));
	}

	/*
	 * Get ALL fields for a selected version, including inactive fields.
	 */
	@GetMapping("/Fields/All")
	public ResponseEntity<List<ClientFormField>> getAllFieldsByVersion(@RequestParam("formversion") Integer version) {

		return ResponseEntity.ok(repository.findByFormversion(version));
	}

	/*
	 * Deactivate one field by its database ID.
	 */
	@PatchMapping("/Field/{id}/Deactivate")
	public ResponseEntity<?> deactivateField(@PathVariable Long id) {

		return repository.findById(id).map(field -> {
			field.setActive(false);
			ClientFormField updated = repository.save(field);

			return ResponseEntity.ok(updated);
		}).orElseGet(() -> ResponseEntity.notFound().build());
	}

	// API 1: Get all existing form versions
	@GetMapping("/Versions")
	public ResponseEntity<List<Integer>> getVersions() {

		return ResponseEntity.ok(repository.findDistinctFormVersions());
	}

	// API 2: Search labels within one form version
	@GetMapping("/Fields/Search")
	public ResponseEntity<List<ClientFormField>> searchFields(@RequestParam Integer version,
			@RequestParam String label) {

		if (label.isBlank()) {
			return ResponseEntity.ok(List.of());
		}

		return ResponseEntity.ok(repository.findByFormversionAndFieldLabelContainingIgnoreCase(version, label.trim()));
	}

}
