public class Main {
    private static final String intervalChar = " "; //二进制浮点字符串中的间隔符，便于查看和处理
    private static long E = 0;//指数值
    private static final long bias = 127;//指数偏移值
    private static long e = 0;//十进制指数存储值 = 指数值 + 指数便宜值

    public static void main(String[] args) {
        String binaryFloatPointArray = "";//二进制浮点字符串

        //不同的输入值
        String input = "0.000000000000000000000000000000000000003";
        /*String input = "0.987654321";*/
        /*String input = "8.125";*/

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
}
