package skc.client.project.SKC_Backend_Files.Service;

import java.io.IOException;
import java.sql.Date;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import skc.client.project.SKC_Backend_Files.Repo.AdminRepo;
import skc.client.project.SKC_Backend_Files.Repo.ClientRepo;
import skc.client.project.SKC_Backend_Files.Repo.FileRepo;
import skc.client.project.SKC_Backend_Files.Wrapper.AuthUser;
import skc.client.project.SKC_Backend_Files.dto.AdditionalFileInfo;
import skc.client.project.SKC_Backend_Files.dto.PresignUploadInfo;
import skc.client.project.SKC_Backend_Files.entity.Admin;
import skc.client.project.SKC_Backend_Files.entity.Client;
import skc.client.project.SKC_Backend_Files.entity.FileEntity;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
public class FileHandling {

	@Autowired
	private S3Client s3Client;
	
	@Autowired
	private FileRepo fileRepo;
	
	@Autowired 
	private AdminRepo adminRepo;
	
	@Autowired
	private ClientRepo clientRepo;

	@Autowired
	private S3Presigner presigner;
	
	@Value("${aws.bucketName}")
	private String bucketname;
	
	
	public String setFile(MultipartFile file,Long id) throws IOException {
		
		FileEntity entity= new FileEntity();
		
		entity.setFileName(file.getOriginalFilename());
		entity.setContentType(file.getContentType());
		entity.setSize(file.getSize());
		
		entity.setCreatedAt( new Date (System.currentTimeMillis()));
		
//		entity.setUserName(userdetails.getName());
//		entity.setUserEmail(userdetails.getEmail());
//		entity.setUserMobile(userdetails.getMobile());
//		entity.setStatus(userdetails.getStatus());
		
		String objectKey = UUID.randomUUID() + "_" + file.getOriginalFilename();
		
		PutObjectRequest request =
		        PutObjectRequest.builder()
		                .bucket("karthikeyan-audit-dev")
		                .key(objectKey)
		                .contentType(file.getContentType())
		                .build();
		
		s3Client.putObject(
		        request,
		        RequestBody.fromBytes(file.getBytes())
		);
		
		entity.setObjectKey(objectKey);
		
		fileRepo.save(entity);
		return "File Saved Successfully";	
	}
	
	public ResponseEntity<byte []> downloadfile(Long id){
		
		FileEntity file=fileRepo.findById(id).orElseThrow();
		
		String objkey=file.getObjectKey();
		
		GetObjectRequest objrequest= GetObjectRequest.builder().bucket("karthikeyan-audit-dev")
				.key(objkey).build();
		
		ResponseBytes<GetObjectResponse> objBytes = s3Client.getObjectAsBytes(objrequest); //this download the file from AWS 
		
		byte[] data=objBytes.asByteArray(); // Store the file into Spring Memory
		
		return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\""+file.getFileName()+"\"").contentType(MediaType
				.parseMediaType(file.getContentType())).body(data);
		
		
	}
	
	
	
	public ResponseEntity<Object> GenerateUploadURL(PresignUploadInfo fileInfo , AuthUser user ){
		
		// create the Object Key
		Date timeStamp = new Date(System.currentTimeMillis());
		
		String Key = user.getRole()+"/"+user.getUsername()+"/"+fileInfo.getFilename()+"/"+fileInfo.getContenttype()+"/"+timeStamp;
		
		PutObjectRequest putrequest = PutObjectRequest.builder().bucket(bucketname).key(Key).build();
		
		PutObjectPresignRequest presignrequest = PutObjectPresignRequest.builder().signatureDuration(Duration.ofMinutes(2)).putObjectRequest(putrequest).build();
		
		PresignedPutObjectRequest request = presigner.presignPutObject(presignrequest);
		
		String Url = request.url().toString();
		
		Map<String,String >response= new HashMap();
		
		if(Url.isEmpty()) {
			response.put("status", "failed");
			response.put("message","Temporary Url Generation Failed , Try Again ");
			return ResponseEntity.badRequest().body(response);
		}
		
		response.put("status", "success");
		response.put("Url", Url);
		response.put("ObjectKey", Key);
		return ResponseEntity.ok(response);
		
	}
	
	
	
	
	
	
	
	
	
	
	
	
	

}
