package csdn.itsaysay.demo.mybatis.mapper;

import csdn.itsaysay.demo.mybatis.bean.User;
import org.apache.ibatis.annotations.*;

@Mapper
public interface TestMapper {

    @Select("select * from usertb where id = #{idd}")
    User selectById(@Param("idd") Integer idd);

    User selectByIdXml(@Param("id") Integer id);

    @Insert("insert into usertb(name,age) values(#{user.name},#{user.age})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(@Param("user") User user);

    @Delete("delete from usertb where id = #{id}")
    void deleteById(@Param("id") Integer id);
}
