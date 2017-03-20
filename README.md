# 前言

IEEE-754 标准是 IEEE 邀请 William Kahan 教授作为顾问帮忙设计的处理器浮点数标准，目前几乎所有计算机都支持这一标准，它大大提高了科学应用程序尤其是针对浮点数的程序在不同机器上的可移植性。

首先我们应该明白，计算机存储数字是以 bit 位为基本元素存储的二进制数字，举个例子，在一个 8 位处理器上，存储器存储整数 7 的方式如下

| 7 |6|5|4|3 |2 |1 |0|
|--|--|--|--|--|--|--|--|
|0|0|0|0|0|1|1|1|

目前广泛使用的 32 位处理器和 64 位处理器都是这样存储整数的，当然其中还涉及到 __大端小端__ 问题，为了便于讨论，后面将一律采取大端法。

而二进制小数的书写方式也可以从二进制整数中类推而来，对于一个二进制小数 abc.def，它的值定义为

```
abc.de = a * 2^(2) + b * 2^(1) + c * 2^(0) + d * 2^(-1) + e * 2^(-2)
```

这里可以看出，二进制小数不能准确表达十进制中的小数，特别是对于不是2的幂次的小数，是无法通过有限个二进制位精确表示的，所以只能采取近似的方式表达。

同时，由于存储器上的每一个 bit 位都只有 0 和 1 两个值，计算机对小数点的存储和识别就成为了一个难题，而 IEEE-754 标准则采取了巧妙的方式解决了这个问题。

# IEEE-754 标准

## 浮点数表达式

IEEE-754 标准将任意一个浮点数（包含小数部分的数字）通过下面的公式表达

```
V = (-1)^s * M * 2^E
```

* 符号-s 决定正负
* 尾数-M 一个二进制小数
* 阶码-E 对浮点数加权

## 存储一个浮点数

在处理器中，一个内存单元被划分成下面三部分用来存放一个浮点数

对于单精度浮点格式（32 位）

|31|30 - 23|22 - 0|
|-|-|-|
|s|exp|frac|

对于双精度浮点格式（64 位）

|63|62 - 52|51 - 0|
|-|-|-|
|s|exp|frac|

其中包含三个字段：

* 1 个单独的 s 符号位
* k 位阶码字段，与 E 相关
* n 位小数字段，与尾数 M 相关

以下我们以单精度浮点数为例进行讨论。单精度浮点数定义了 1 位符号位，8 位阶码字段，23 位小数字段。

根据阶码字段的不同，我们可以将浮点数分为 4 类：

##### 1. 规格化浮点数

|31|30 - 23|22 - 0|
|-|-|-|
|s|!= 0 & != 255|frac|

##### 2. 非规格化浮点数

|31|30 - 23|22 - 0|
|-|-|-|
|s|00000000|frac|

##### 3. 无穷大

|31|30 - 23|22 - 0|
|-|-|-|
|s|11111111|000……000|

##### 4. NaN(Not a Number)

|31|30 - 23|22 - 0|
|-|-|-|
|s|11111111|!= 0|

## 规格化浮点数

对于规格化浮点数，阶码值 __E = e - Bias__ ， 其中 e 就是阶码段表示的数字，而 __Bias = 2^k-1^ - 1__ ，在单精度中是127，双精度中是1023。因此单精度中阶码值 E 的范围是 [-126, +127]，双精度中阶码值 E 的范围是 [-1022, +1023]。

对于小数字段 frac，形式为 __f~n-1~ f~n-2~ f~n-3~ …… f~2~ f~1~ f~0~__ ,它所表示的二进制值是 __f = 0.f~n-1~ f~n-2~ f~n-3~ …… f~2~ f~1~ f~0~__ 。而尾数 __M = f + 1__ 。

可以看出，其实这里我们将浮点数首位默认为 1，所以没有显式地存储在存储器中。之所以能够这样做，是因为我们可以通过调整阶码 E 的值，使得二进制小数部分落在 1 和 2 之间，从而可以获得一个额外的精度位。

举个例子，对于一个浮点数 0.10111，原本我们需要存储 10111 五位小数位，我们可以表示为

```1.0111 * 2^(-1)```

这样我们其实只需要存储 0111 四位就可以，因为我们默认了浮点数的首位为 1。

## 非规格化浮点数

对于非规格化符段数，定义阶码值 __E = 1 - Bias__ ，定义尾数 __M = f = 0.f~n-1~ f~n-2~ f~n-3~ …… f~2~ f~1~ f~0~__ 。

## 特殊值

剩下两种特殊值，无穷大和 NaN 的表示比较简单，这里就不说明了。

## 表示范围

首先要明白有限位的浮点数在数轴上的分布是稀疏的，至于具体的分布情况，可以参考这个问题 [计算机中的浮点数在数轴上分布均匀吗？](https://www.zhihu.com/question/21645386)

而标准可以表示的范围，在单精度下，是 [-2 ^ (127), 2 ^ (127)]，双精度下是 [-2 ^ (1023), 2 ^ (1023)]。

同时我们也可以知道，规范化浮点数表示的是绝对值大于 2 ^ (-126) 或 2 ^ (-1022) 的数字，非规范化浮点数表示的则是小于 2 ^ (-126) 或 2 ^ (-1022) 的值。

# 标准实现

## 三类转换过程

对于一个给定的十进制数字 input，根据 IEEE-754 标准，我们又可以分成三种情况，为了方便讨论，我们定义 input > 0，在单精度下表示。

##### 1. input > 1

显而易见，应该用规范化浮点数来表示。

这里我们首先确定 input 的二进制形式的整数位数，从而确定阶码应该为多少，然后计算出阶码存储值和小数段。

举个例子，假设 input = 8.25，那么其二进制形式为 1000.01，要转换为 1.000001 * 2 ^ (3) 的形式，故阶码值为 3，阶码存储值为 __e = 3 + 127 = 130__，转换为二进制形式为 __1000010__，小数部分则为 __00001__。


##### 2. input < 1 && input > 2 ^ (-126)

这里依然要用规范化浮点数来表示。

首先定位 input 的二进制形式中，小数部分第一个 1 出现的位置，从而可以确定阶码值。

举个例子，假设 input = 0.25，那么它的二进制形式为 0.01，所以要表示成 1.0 * 2 ^ (-2) 的形式，故阶码值为 -2，阶码存储值为 __e = -2 + 127 = 125__ ，转换为二进制形式为 __01111101__，小数部分全为零。

##### 3. input < 2 ^ (-126)

此时我们需要用非规范化浮点数表示，由于阶码值固定为 -126，所以其实只要想办法把 input 转换为 __f * 2 ^ (-126)__ 的形式，再把 f 转换为二进制就可以了。

## 验证转换过程

想知道我们的实现是否正确，最简单的方式就是逆向转换过程，通过二进制浮点数字符串，看看能否计算出我们的输入值。

## Java代码实现（基于单精度浮点格式）

```Java
    private static final String intervalChar = " "; //二进制浮点字符串中的间隔符，便于查看和处理
    private static long E = 0;//指数值
    private static final long bias = 127;//指数偏移值
    private static long e = 0;//十进制指数存储值 = 指数值 + 指数便宜值

    public static void main(String[] args) {
        String binaryFloatPointArray = "";//二进制浮点字符串

        //不同的输入值
        String input = "0.000000000000000000000000000000000000002";
        /*String input = "0.987654321";*/
        /*String input = "8.0";*/

        String[] temp = input.split("\\.");//用小数点分割输入值
        long integerPart = Long.parseLong(temp[0]);
        double decimalPart = Double.parseDouble("0." + temp[1]);
        String binaryIntegerString = Long.toBinaryString(integerPart);//十进制整数转换为二进制整数字符串
        String binaryDecimalString = doubleToBinaryString(decimalPart);//十进制小数转换为二进制小数字符串
        binaryFloatPointArray = getFloatPointArray(binaryIntegerString, binaryDecimalString);//获得二进制浮点字符串
        System.out.println("Float Point Array: " + binaryFloatPointArray);

        System.out.println("Original Input: " + getOriginalInput(binaryFloatPointArray));//逆向获得二进制浮点字符串对应的十进制小数值
    }


    /**
     * 输入：二进制整数字符串，二进制小数字符串
     * 输出：IEEE 754标准的二进制浮点数字符串
    **/
    private static String getFloatPointArray(String binaryIntegerString, String binaryDecimalString){
        String result = "";
        if (!binaryIntegerString.equals("0")){ //输入值 > 1
            E = binaryIntegerString.length() - 1;//获得小数点前移的位数
            e = E + bias;//十进制指数存储值
            result = "0" + intervalChar
                    + autoCompleteBinaryExponentArray(Long.toBinaryString(e)) +
                    intervalChar
                    + autoCompleteBinaryDecimalArray(binaryIntegerString.substring(1, binaryIntegerString.length()) + binaryDecimalString);
        } else {
            if (binaryDecimalString.indexOf("1") >= (126 - 1)){ //输入值 <= 2^(-125)
                result = "0"
                        + intervalChar
                        + "00000000"
                        + intervalChar
                        + autoCompleteBinaryDecimalArray(binaryDecimalString.substring(126, binaryDecimalString.length()));
            } else { //输入值介于 2^(-125) 与 1 之间
                E = binaryDecimalString.indexOf("1") + 1;
                e = 0 - E + bias;
                result = "0"
                        + intervalChar
                        + autoCompleteBinaryExponentArray(Long.toBinaryString(e))
                        + intervalChar
                        + autoCompleteBinaryDecimalArray(binaryDecimalString.substring((int) E, binaryDecimalString.length()));
            }
        }
        return result;
    }


    /**
     * 输入：二进制浮点数字符串
     * 输出：double类型的十进制小数值
     ***/
    private static double getOriginalInput(String floatPointArray){
        String[] results = floatPointArray.split(intervalChar);
        double originInput = 0.0;
        if (results[1].equals("00000000")){ //非规格化值
            originInput = binaryStringToDouble(results[2]) * Math.pow(2, -126);
        } else if (!results[1].equals("11111111")){ //规格化值
            originInput = (binaryStringToDouble(results[2]) + 1) * Math.pow(2, Integer.valueOf(results[1], 2) - bias);
        }
        return originInput;
    }

    private static String doubleToBinaryString(double input){
        String result = "";
        int temp = 0;
        for (int i = 0; i < 150; i++){
            temp = (int) (input * 2);
            input = input * 2 - (double)temp;
            result += temp;
        }
        return result;
    }

    private static double binaryStringToDouble(String input){
        double output = 0;
        for (int i = 0; i < input.length(); i++){
            output += (Double.parseDouble(String.valueOf(input.charAt(i)))) /(Math.pow(2, i + 1));
        }
        return output;
    }

    private static String autoCompleteBinaryExponentArray(String input){
        String temp = "00000000";//8 zeros
        if (input.length() > 8){
            System.out.println("Overflow Error in Exponent Part");
        }
        return temp.substring(0, 8 - input.length()) + input;
    }

    private static String autoCompleteBinaryDecimalArray(String input){
        String temp = "00000000000000000000000";//23 zeros
        if (input.length() > 23){
            return input.substring(0, 23);
        } else {
            return input + temp.substring(0, 23 - input.length());
        }
    }
```

代码里有一些工具类方法，具体包括 double 值与二进制字符串的相互转换、输出字符串的自动补全等。

主要的转换过程就是 __getFloatPointArray()__ 和 __getOriginalInput()__，前者获得十进制输入值的二进制浮点数字符串，后者获得二进制浮点字符串对应的原十进制输入数字。

接下来看看运行情况

* input = "8.125"

输出如下

```
Float Point Array: 0 10000010 00000100000000000000000
Original Input: 8.125
```

* input = "0.987654321"

输出如下

```
Float Point Array: 0 01111110 11111001101011011101001
Original Input: 0.9876542687416077
```

* input = "0.000000000000000000000000000000000000003" (小数点后 38 个 0)

输出如下

```
Float Point Array: 0 00000000 01000001010101011000111
Original Input: 2.9999992446175354E-39
```

# 补充

IEEE 754 浮点表示标准有许多有趣的特性，比如前面提到的在数轴上的分布，还有对于 0 的表示、浮点数的偶数舍入、浮点数运算等等，可以对比计算机对于整数的表示方法。

# 参考

* 《深入理解计算机系统》第二章
* [浮点数的二进制表示-阮一峰](http://www.ruanyifeng.com/blog/2010/06/ieee_floating-point_representation.html)
