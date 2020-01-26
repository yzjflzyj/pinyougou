import com.pinyougou.seckill.service.impl.MultiThreadWork;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath*:spring/applicationContext-*.xml")
public class MultiThreadTest {

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    private MultiThreadWork multiThreadWork;

    @Test
    public void testMultiThread(){
        for (int i = 0; i < 5; i++) {
            System.out.println("循环" + i + "开始....");
            //多线程调用
            multiThreadWork.doSomeThing(i);
            System.out.println("循环" + i + "结束....");
        }
        try {
            //主线程阻塞，等待其它子线程执行
            System.in.read();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
