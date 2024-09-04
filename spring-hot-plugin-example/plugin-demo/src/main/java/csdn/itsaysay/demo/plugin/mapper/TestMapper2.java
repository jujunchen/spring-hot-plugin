package csdn.itsaysay.demo.plugin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import csdn.itsaysay.demo.plugin.bean.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TestMapper2 extends BaseMapper<User> {
}
