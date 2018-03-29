//
//  ViewController.swift
//  iostest
//
//  Created by Gust on 2017/10/6.
//  Copyright © 2017年 Gust. All rights reserved.
//

import UIKit

class ViewController: UIViewController {
    @IBOutlet weak var tv: UITextView!

    override func viewDidLoad() {
        super.viewDidLoad()
        // Do any additional setup after loading the view, typically from a nib.
        
        let screenSize = UIScreen.main.bounds
        let lab = UILabel.init(frame:CGRect(x:0, y:30, width:screenSize.width, height:20))
        lab.text="stderr:"
        self.view.addSubview(lab)
        let textview = UITextView.init(frame:CGRect(x:0, y:50, width:screenSize.width, height:screenSize.height-100))
        tv=textview;
        textview.layer.borderWidth = 1  //边框粗细
        textview.layer.borderColor = UIColor.gray.cgColor //边框颜色
        textview.font = .systemFont(ofSize: 16)
        textview.isEditable=false
        self.view.addSubview(textview)
        textview.text = "press buttom show log"
        
        //创建一个ContactAdd类型的按钮
        let button:UIButton = UIButton(type:.contactAdd)
        //设置按钮位置和大小
        button.frame = CGRect(x:0, y:screenSize.height-50, width:screenSize.width, height:50)
        //设置按钮文字
        button.setTitle("show log", for:.normal)
        self.view.addSubview(button)
        button.addTarget(self, action:#selector(tapped(_:)), for:.touchUpInside)
        
        let myString = "in main!"
        print(myString)
                
        //_ = Timer.scheduledTimer(timeInterval: 1, target: self, selector: #selector(self.show_output), userInfo: nil, repeats: true)
        execjvm()
        
        
    }
    
    

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    
    @objc func show_output()    {
        self.tv.text = self.readFromDocumentsFile(fileName: "console.txt")
    }
    
    @objc func tapped(_ button:UIButton){
        print(button.title(for: .normal))
        //tv.text = "start..."
        show_output()
    }
    
    @objc func execjvm()    {
       
        let mainBundle = Bundle.main.bundlePath
        let app_path=mainBundle.cString(using: String.Encoding.utf8)
        call_jvm(UnsafeMutablePointer<Int8>(mutating: app_path));
    }

    
    func readFromDocumentsFile(fileName:String) -> String {
        let documentsPath = NSSearchPathForDirectoriesInDomains(.documentDirectory, .userDomainMask, true)[0] as NSString
        let path = documentsPath.appendingPathComponent(fileName)
        let checkValidation = FileManager.default
        var file:String
        
        if checkValidation.fileExists(atPath: path) {
            do{
                try file = NSString(contentsOfFile: path, encoding: String.Encoding.utf8.rawValue) as String
            }catch{
                file = ""
            }
        } else {
            file = ""
        }
        return file
    }

}


