import Foundation
import Darwin // 引入Darwin库以使用strdup和free函数

// 假设这个函数已经在其他地方定义
// void call_jvm(char *path);

func execjvm() {
    let mainBundle = Bundle.main.bundlePath
    var appPath: UnsafeMutablePointer<CChar>? = nil
    mainBundle.withCString { originalPath in
        appPath = strdup(originalPath)
    }
    
    if let validAppPath = appPath {
        call_jvm(validAppPath)
        // 使用完后释放内存
        free(validAppPath)
    } else {
        print("Failed to convert the bundle path to a C string.")
    }
}

print("Hello, World!")
execjvm()
