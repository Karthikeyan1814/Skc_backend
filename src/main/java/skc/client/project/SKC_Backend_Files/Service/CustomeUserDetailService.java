package skc.client.project.SKC_Backend_Files.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import skc.client.project.SKC_Backend_Files.Repo.AdminRepo;
import skc.client.project.SKC_Backend_Files.Repo.ClientRepo;
import skc.client.project.SKC_Backend_Files.Wrapper.AuthUser;
import skc.client.project.SKC_Backend_Files.entity.Admin;
import skc.client.project.SKC_Backend_Files.entity.Client;

@Service
public class CustomeUserDetailService implements UserDetailsService {
	
	@Autowired
	private AdminRepo adminrepo;
	
	@Autowired
	private ClientRepo clientrepo;

	
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		
		Admin admindetails =  adminrepo.findByMobile(username);
		
		if(admindetails != null) {
			return new AuthUser(
					admindetails.getId(),
					admindetails.getEmail(),
					admindetails.getPassword(),
					admindetails.getRole()
					);
		}
		
		Client client =clientrepo.findByMobile(username);
		
		if(client !=null) {
			return new AuthUser(
					client.getId(),
					client.getEmail(),
					client.getPassword(),
					client.getRole()
					);
		}
		
		  throw new UsernameNotFoundException(
	                "User Not Found"
	        );
	}

	

}
