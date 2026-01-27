    package dev.gmpark.cors.controllers;

    import dev.gmpark.cors.entities.RegisterEntity;
    import dev.gmpark.cors.results.register.RegisterResult;
    import dev.gmpark.cors.services.OwnerMainService;
    import dev.gmpark.cors.services.OwnerMemberService;
    import lombok.RequiredArgsConstructor;
    import org.springframework.http.MediaType;
    import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
    import org.springframework.stereotype.Controller;
    import org.springframework.web.bind.annotation.*;
    import org.springframework.web.servlet.ModelAndView;

    import java.util.HashMap;
    import java.util.List;
    import java.util.Map;

    @Controller
    @RequestMapping(value = "/owner")
    @RequiredArgsConstructor
    public class OwnerMemberController {

        private final OwnerMainService ownerMainService;
        private final OwnerMemberService ownerMemberService;

        private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        /**
         * 사원 관리 페이지 (목록 조회)
         */
        @RequestMapping(value = "/member", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
        public ModelAndView getMember(@SessionAttribute(value = "sessionUser", required = false) RegisterEntity sessionUser,
                                      @RequestParam(value = "level", required = false) Integer level,
                                      @RequestParam(value = "keyword", required = false) String keyword,
                                      ModelAndView modelAndView) {

            if( sessionUser == null) {
              return new ModelAndView("redirect:/login");
            }
            if (!"owner".equalsIgnoreCase(sessionUser.getUsertype())) {
                modelAndView.setViewName("redirect:/main");
                return modelAndView;
            }
            if ( sessionUser.getLevel()==2 || sessionUser.getLevel() == 1) {
                modelAndView.setViewName("redirect:/owner?alert=noauth"); // 예시
                return modelAndView;
            }

            var shop = this.ownerMainService.getShop(sessionUser.getEmail());
            modelAndView.addObject("shop", shop);

            List<RegisterEntity> members = this.ownerMemberService.getMembers(sessionUser.getEmail(), level, keyword);
            modelAndView.addObject("members", members);

            modelAndView.setViewName("ownermember/ownermember");
            return modelAndView;



        }


        /**
         * 신규 사원 등록 API
         */
        @RequestMapping(value = "/member", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
        @ResponseBody
        public Map<String, Object> postMember(@SessionAttribute(value = "sessionUser") RegisterEntity sessionUser,
                                              RegisterEntity newMember) {

            newMember.setOwnerEmail(sessionUser.getEmail());
            RegisterResult result = this.ownerMemberService.addMember(newMember,sessionUser.getEmail(),sessionUser.getLevel());
// 기존:     RegisterResult result = this.ownerMemberService.addMember(newMember);
            Map<String, Object> response = new HashMap<>();
            response.put("result", result.name().toLowerCase());
            response.put("status", result.name().toUpperCase());
            return response;
        }
        @RequestMapping(value = "/member", method = RequestMethod.PATCH, produces = MediaType.APPLICATION_JSON_VALUE)
        @ResponseBody
        public Map<String, Object> patchMember(@SessionAttribute(value = "sessionUser") RegisterEntity sessionUser,
                                               @RequestParam(value = "email") String email,
                                               @RequestParam(value = "name") String name,
                                               @RequestParam(value = "level") int level,
                                               @RequestParam(value = "currentPassword") String currentPassword) { // [수정 2] 오타 수정

            RegisterResult result = this.ownerMemberService.modifyMember(email, name, level, currentPassword,sessionUser.getEmail(),sessionUser.getLevel());
//기존:      RegisterResult result = this.ownerMemberService.modifyMember(email, name, level, currentPassword);
            Map<String, Object> response = new HashMap<>();
            response.put("result", result.name());

            return response;
        }

        @RequestMapping(value = "/member", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
        @ResponseBody
        public Map<String, Object> deleteMember(@SessionAttribute(value = "sessionUser") RegisterEntity sessionUser,
                                                @RequestParam(value = "email") String targetEmail) {
            RegisterResult result = this.ownerMemberService.removeMember(targetEmail, sessionUser.getEmail(), sessionUser.getLevel());
            Map<String, Object> response = new HashMap<>();
            response.put("result", result.name());
            return  response;
        }

    }