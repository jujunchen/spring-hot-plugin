package csdn.itsaysay.main.plugin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import csdn.itsaysay.main.plugin.bean.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TestMapper2 extends BaseMapper<User> {
}
