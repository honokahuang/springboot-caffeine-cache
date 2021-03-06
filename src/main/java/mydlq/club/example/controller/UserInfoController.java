package mydlq.club.example.controller;

import mydlq.club.example.entity.UserInfo;
import mydlq.club.example.service.UserInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 用户信息 Controller
 */
@RestController
@RequestMapping
public class UserInfoController {

    @Autowired
    private UserInfoService userInfoService;

    @GetMapping("/userInfo/{id}")
    public Object getUserInfo(@PathVariable Integer id) {
        UserInfo userInfo = userInfoService.getByName(id);
        if (userInfo == null) {
            return "could not found data";
        }
        return userInfo;
    }

    @PostMapping("/userInfo")
    public Object createUserInfo(@RequestBody UserInfo userInfo) {
        userInfoService.addUserInfo(userInfo);
        return "SUCCESS";
    }

    @PostMapping("/userInfoJ")
    public Object createUserInfoJ(@RequestBody UserInfo userInfo) {
        userInfoService.addUserInfoJ(userInfo);
        return "SUCCESS";
    }

    @PostMapping("/userInfoF")
    public Object createUserInfoF(@RequestBody UserInfo userInfo) {
        userInfoService.addUserInfoF(userInfo);
        return "SUCCESS";
    }

    @PostMapping("/userInfoC")
    public Object createUserInfoC(@RequestBody UserInfo userInfo) {
        userInfoService.addUserInfoC(userInfo);
        return "SUCCESS";
    }


    @PutMapping("/userInfo")
    public Object updateUserInfo(@RequestBody UserInfo userInfo) {

        UserInfo newUserInfo = userInfoService.updateUserInfo(userInfo);
        if (newUserInfo == null){
            return "不存在该用户";
        }
        return newUserInfo;
    }

    @DeleteMapping("/userInfo/{id}")
    public Object deleteUserInfo(@PathVariable Integer id) {
        userInfoService.deleteById(id);
        return "SUCCESS";
    }

}
