package csdn.itsaysay.demo.plugin;

import com.alibaba.fastjson.JSON;
import com.sun.jna.Native;
import csdn.itsaysay.demo.plugin.bean.Person;
import csdn.itsaysay.demo.plugin.bean.User;
import csdn.itsaysay.demo.plugin.hik.HCNetSDK;
import csdn.itsaysay.demo.plugin.service.TestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@Slf4j
@RestController
public class IndexController {

    @Resource
    private TestService testService;

    @GetMapping({"/hello"})
    public String hello() {
        return "Hello itsaysay!";
    }

    @GetMapping({"/hello/name"})
    public Person helloName() {
        Person person = new Person();
        person.setName("Human");
        String json = JSON.toJSONString(person);
        log.info(json);
        return person;
    }

    @GetMapping("/hik/sdk/init")
    public Boolean hikSDKInit() {
        //海康的SDK dll依赖了其他的dll，需要指定目录，放插件包没用
        //只有一个dll的sdk，不需要，可以放在插件包内
        //这是java native的限制
        HCNetSDK hCNetSDK = (HCNetSDK) Native.loadLibrary("D:\\javaprojects\\spring-hot-plugin\\spring-hot-plugin-example\\plugin-demo\\target\\classes\\win32-x86-64\\HCNetSDK.dll", HCNetSDK.class);
        if (hCNetSDK == null) {
            return false;
        }
        return true;
    }

    @GetMapping("/mp/getUser/{id}")
    public User getUser(@PathVariable("id") Integer id) {
        return testService.getUser(id);
    }

    @GetMapping("/mp/getUserXml/{id}")
    public User getUserXml(@PathVariable("id") Integer id) {
        return testService.getUserXml(id);
    }
}
