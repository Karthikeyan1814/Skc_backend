package skc.client.project.SKC_Backend_Files.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import skc.client.project.SKC_Backend_Files.Repo.AdminRepo;
import skc.client.project.SKC_Backend_Files.Repo.ClientRepo;
import skc.client.project.SKC_Backend_Files.Repo.StaffRepo;
import skc.client.project.SKC_Backend_Files.Wrapper.AuthUser;
import skc.client.project.SKC_Backend_Files.entity.Admin;
import skc.client.project.SKC_Backend_Files.entity.Client;
import skc.client.project.SKC_Backend_Files.entity.Staff;

@Service
public class CustomeUserDetailService implements UserDetailsService {

    @Autowired private AdminRepo adminrepo;
    @Autowired private ClientRepo clientrepo;
    @Autowired private StaffRepo staffrepo;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        // Try admin first
        Admin admindetails = adminrepo.findByMobile(username);
        if (admindetails != null) {
            return new AuthUser(admindetails.getId(), admindetails.getMobile(),
                    admindetails.getPassword(), admindetails.getRole());
        }

        // Try staff
        Staff staffdetails = staffrepo.findByMobile(username);
        if (staffdetails != null) {
            return new AuthUser(staffdetails.getId(), staffdetails.getMobile(),
                    staffdetails.getPassword(), staffdetails.getRole());
        }

        // Try client
        Client client = clientrepo.findByMobile(username);
        if (client != null) {
            return new AuthUser(client.getId(), client.getMobile(),
                    client.getPassword(), client.getRole());
        }

        throw new UsernameNotFoundException("User not found: " + username);
    }
}
