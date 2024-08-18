package csdn.itsaysay.plugin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import csdn.itsaysay.plugin.bean.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TestMapper extends BaseMapper<User> {

    User selectByIdXml(@Param("id") Integer id);
}
