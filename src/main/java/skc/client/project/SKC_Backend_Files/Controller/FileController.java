package skc.client.project.SKC_Backend_Files.Controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import skc.client.project.SKC_Backend_Files.Service.FileHandling;
import skc.client.project.SKC_Backend_Files.Wrapper.AuthUser;
import skc.client.project.SKC_Backend_Files.dto.PresignUploadInfo;

@RestController
@RequestMapping("/skc/Admin")
public class FileController {

	@Autowired
	private FileHandling fileServices;
	
	@PostMapping("/Upload")
	public String uploadFile(@RequestPart("file") MultipartFile file , @RequestPart("id") Long id) throws IOException{
		return fileServices.setFile(file, id);
		//return fileServices.tempUploadClientFile(file, details,user);
	}
	

	
	@PostMapping("/TempUpload")
	public ResponseEntity<Object> PresignUpload(@RequestBody PresignUploadInfo fileInfo , @AuthenticationPrincipal AuthUser user ) {
		return fileServices.GenerateUploadURL(fileInfo,user);
	}
	

}