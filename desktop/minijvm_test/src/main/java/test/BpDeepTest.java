package test;

import java.util.Random;

public class BpDeepTest {

    static String arr2str(double[] arr) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arr.length; i++) {
            sb.append("[").append(i).append("]").append(arr[i]).append(',');
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        //初始化神经网络的基本配置
        //第一个参数是一个整型数组，表示神经网络的层数和每层节点数，比如{3,10,10,10,10,2}表示输入层是3个节点，输出层是2个节点，中间有4层隐含层，每层10个节点
        //第二个参数是学习步长，第三个参数是动量系数
        BpDeep bp = new BpDeep(new int[]{2, 10, 2}, 0.15, 0.8);

        //设置样本数据，对应上面的4个二维坐标数据
        double[][] data = new double[][]{{1, 2}, {2, 2}, {1, 1}, {2, 1}};
        //设置目标数据，对应4个坐标数据的分类
        double[][] target = new double[][]{{1, 0}, {0, 1}, {0, 1}, {1, 0}};

        //迭代训练5000次
        for (int n = 0; n < 5000; n++) {
            for (int i = 0; i < data.length; i++) {
                bp.train(data[i], target[i]);
            }
        }

        //根据训练结果来检验样本数据
        for (int j = 0; j < data.length; j++) {
            double[] result = bp.computeOut(data[j]);
            System.out.println(arr2str(data[j]) + ":" + arr2str(result));
        }

        //根据训练结果来预测一条新数据的分类
        double[] x = new double[]{3, 1};
        double[] result = bp.computeOut(x);
        System.out.println(arr2str(x) + ":" + arr2str(result));

        System.out.println("cost :" + (System.currentTimeMillis() - start));
    }
}

/**
 * http://blog.csdn.net/u011922760/article/details/52785383
 *
 * @author Gust
 */
class BpDeep {

    public double[][] layer;//神经网络各层节点
    public double[][] layerErr;//神经网络各节点误差
    public double[][][] layer_weight;//各层节点权重
    public double[][][] layer_weight_delta;//各层节点权重动量
    public double mobp;//动量系数
    public double rate;//学习系数

    public BpDeep(int[] layernum, double rate, double mobp) {
        this.mobp = mobp;
        this.rate = rate;
        layer = new double[layernum.length][];
        layerErr = new double[layernum.length][];
        layer_weight = new double[layernum.length][][];
        layer_weight_delta = new double[layernum.length][][];
        Random random = new Random();
        for (int l = 0; l < layernum.length; l++) {
            layer[l] = new double[layernum[l]];
            layerErr[l] = new double[layernum[l]];
            if (l + 1 < layernum.length) {
                layer_weight[l] = new double[layernum[l] + 1][layernum[l + 1]];
                layer_weight_delta[l] = new double[layernum[l] + 1][layernum[l + 1]];
                for (int j = 0; j < layernum[l] + 1; j++) {
                    for (int i = 0; i < layernum[l + 1]; i++) {
                        layer_weight[l][j][i] = random.nextDouble();//随机初始化权重
                    }
                }
            }
        }
    }

    //逐层向前计算输出
    public double[] computeOut(double[] in) {
        for (int l = 1; l < layer.length; l++) {
            for (int j = 0; j < layer[l].length; j++) {
                double z = layer_weight[l - 1][layer[l - 1].length][j];
                for (int i = 0; i < layer[l - 1].length; i++) {
                    layer[l - 1][i] = l == 1 ? in[i] : layer[l - 1][i];
                    z += layer_weight[l - 1][i][j] * layer[l - 1][i];
                }
                layer[l][j] = 1 / (1 + Math.exp(-z));
            }
        }
        return layer[layer.length - 1];
    }

    //逐层反向计算误差并修改权重
    public void updateWeight(double[] tar) {
        int l = layer.length - 1;
        for (int j = 0; j < layerErr[l].length; j++) {
            layerErr[l][j] = layer[l][j] * (1 - layer[l][j]) * (tar[j] - layer[l][j]);
        }

        while (l-- > 0) {
            for (int j = 0; j < layerErr[l].length; j++) {
                double z = 0.0;
                for (int i = 0; i < layerErr[l + 1].length; i++) {
                    z = z + l > 0 ? layerErr[l + 1][i] * layer_weight[l][j][i] : 0;
                    layer_weight_delta[l][j][i] = mobp * layer_weight_delta[l][j][i] + rate * layerErr[l + 1][i] * layer[l][j];//隐含层动量调整
                    layer_weight[l][j][i] += layer_weight_delta[l][j][i];//隐含层权重调整
                    if (j == layerErr[l].length - 1) {
                        layer_weight_delta[l][j + 1][i] = mobp * layer_weight_delta[l][j + 1][i] + rate * layerErr[l + 1][i];//截距动量调整
                        layer_weight[l][j + 1][i] += layer_weight_delta[l][j + 1][i];//截距权重调整
                    }
                }
                layerErr[l][j] = z * layer[l][j] * (1 - layer[l][j]);//记录误差
            }
        }
    }

    public void train(double[] in, double[] tar) {
        double[] out = computeOut(in);
        updateWeight(tar);
    }
}
