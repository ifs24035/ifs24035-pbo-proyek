package org.delcom.app.views;

import org.delcom.app.entities.User;
import org.delcom.app.services.PetService;
import org.delcom.app.utils.ConstUtil;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@Controller
public class HomeView {

    private final PetService petService;

    public HomeView(PetService petService) {
        this.petService = petService;
    }

    @GetMapping("/about")
    public String about() {
        return ConstUtil.TEMPLATE_PAGES_ABOUT;
    }

    @GetMapping("/")
    public String home(Model model, @AuthenticationPrincipal User user) {
        // user otomatis di-inject oleh Spring Security dari AuthView login session
        if (user == null) {
            return "redirect:/auth/login";
        }

        model.addAttribute("userName", user.getName());
        model.addAttribute("shopName", "Petshop Delcom"); // Bisa dinamis kalau mau
        
        // Fitur Daftar Data
        model.addAttribute("pets", petService.getAllPetsByUser(user.getId()));

        // Fitur Chart Data
        Map<String, Long> stats = petService.getPetTypeStats(user.getId());
        model.addAttribute("chartLabels", stats.keySet());
        model.addAttribute("chartData", stats.values());

        return ConstUtil.TEMPLATE_PAGES_HOME;
    }
}