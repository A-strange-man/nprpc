/**
 * 基于接口的事件回调操作
 *
 * 模拟界面类
 */
public class GuiTestCase implements INotifyCallBack{
    private Download download;

    public GuiTestCase() {
        this.download = new Download(this);
    }

    public void downLoadFile(String file) throws InterruptedException {
        System.out.println("begin start download file: " + file);
        download.start(file);
    }

    @Override
    public void progres(String file, int progress) {
        System.out.println(file + "download progress: " + progress + "%");
    }

    @Override
    public void result(String file) {
        System.out.println(file + " download over!");
    }

    public static void main(String[] args) throws InterruptedException {
        GuiTestCase g = new GuiTestCase();
        g.downLoadFile("jdk");
    }
}

interface INotifyCallBack {
    void progres(String file, int progress);
    void result(String file);
}

class Download {
    private INotifyCallBack cb;
    public Download(INotifyCallBack cb) {
        this.cb = cb;
    }

    public void start(String file) throws InterruptedException {
        int count = 0;
        while (count <= 100) {
            cb.progres(file, count);
            Thread.sleep(1000);
            count += 20;
        }
        cb.result(file);
    }
}
