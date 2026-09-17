package skc.client.project.SKC_Backend_Files.dto;

public class PresignUploadInfo {

	
	private String filename;
	private String contenttype;
	public String getFilename() {
		return filename;
	}
	public void setFilename(String filename) {
		this.filename = filename;
	}
	public String getContenttype() {
		return contenttype;
	}
	public void setContenttype(String contenttype) {
		this.contenttype = contenttype;
	}
	public PresignUploadInfo(String filename, String contenttype) {
		super();
		this.filename = filename;
		this.contenttype = contenttype;
	}
	
}
