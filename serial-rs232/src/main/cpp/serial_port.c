
#include <strings.h>
#include <termios.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <string.h>
#include <jni.h>
#include "serial_port.h"

static speed_t getBaudrate(jint baudrate)
{
    switch(baudrate) {
        case 0: return B0;
        case 50: return B50;
        case 75: return B75;
        case 110: return B110;
        case 134: return B134;
        case 150: return B150;
        case 200: return B200;
        case 300: return B300;
        case 600: return B600;
        case 1200: return B1200;
        case 1800: return B1800;
        case 2400: return B2400;
        case 4800: return B4800;
        case 9600: return B9600;
        case 19200: return B19200;
        case 38400: return B38400;
        case 57600: return B57600;
        case 115200: return B115200;
        case 230400: return B230400;
        case 460800: return B460800;
        case 500000: return B500000;
        case 576000: return B576000;
        case 921600: return B921600;
        case 1000000: return B1000000;
        case 1152000: return B1152000;
        case 1500000: return B1500000;
        case 2000000: return B2000000;
        case 2500000: return B2500000;
        case 3000000: return B3000000;
        case 3500000: return B3500000;
        case 4000000: return B4000000;
        default: return (speed_t) -1;
    }
}


/**
 * 设置串口数据，校验位,速率，停止位
 * @param speed 波特率
 * @param nBits 类型 int数据位 取值 位7或8
 * @param nEvent 类型 char 校验类型 取值N ,E, O,,S
 * @param mStop 类型 int 停止位 取值1 或者 2
 */
int set_opt(int fd, speed_t speed, jint nBits, jint nEvent, jint nStop)
{
    LOGD("set_opt:nBits=%d, nEvent=%d, nStop=%d", nBits, nEvent, nStop);
    struct termios newtio, oldtio;

    /*保存测试现有串口参数设置，在这里如果串口号等出错，会有相关的出错信息*/
    if ((tcgetattr(fd, &oldtio)) != 0)
    {
        LOGE("tcgetattr( fd,&oldtio) -> %d\n",tcgetattr( fd,&oldtio));
        return -1;
    }
    bzero(&newtio, sizeof(newtio));

    /*步骤一，设置字符大小*/
    newtio.c_cflag  |=  CLOCAL | CREAD;
    newtio.c_cflag &= ~CSIZE;

    /*设置停止位*/
    switch(nBits)
    {
        case 8:
//            newtio.c_cflag &= ~CSIZE;
            newtio.c_cflag |= CS8;
            break;
        case 7:
//            newtio.c_cflag &= ~CSIZE;
            newtio.c_cflag |= CS7;
            break;
        case 6:
//            newtio.c_cflag &= ~CSIZE;
            newtio.c_cflag |= CS6;
            break;
        case 5:
//            newtio.c_cflag &= ~CSIZE;
            newtio.c_cflag |= CS5;
        default:
            break;
    }

    /*设置奇偶校验位*/
    switch( nEvent )
    {
        case 'o':
        case 'O': //奇数
            newtio.c_cflag |= PARENB;
            newtio.c_cflag |= PARODD;
            newtio.c_iflag |= (INPCK | ISTRIP);
            break;
        case 'e':
        case 'E': //偶数
            newtio.c_iflag |= (INPCK | ISTRIP);
            newtio.c_cflag |= PARENB;
            newtio.c_cflag &= ~PARODD;
            break;
        case 'n':
        case 'N':  //无奇偶校验位
            newtio.c_cflag &= ~PARENB;
            break;
        default:
            break;
    }

    /* 设置波特率 */
    cfsetispeed(&newtio, speed);
    cfsetospeed(&newtio, speed);

    /* 设置停止位 */
    if(nStop == 1)
        newtio.c_cflag &=  ~CSTOPB;
    else if (nStop == 2)
        newtio.c_cflag |=  CSTOPB;

    /*设置等待时间和最小接收字符*/
    newtio.c_cc[VTIME]  = 0;
    newtio.c_cc[VMIN] = 0;

    /*处理未接收字符*/
    tcflush(fd, TCIFLUSH);

    /*激活新配置*/
    if((tcsetattr(fd,TCSANOW,&newtio))!=0)
    {
        LOGW("com set error");
        return -1;
    }
    LOGD("set done!\n");
    return 0;
}

jobject
Java_com_seiko_serial_rs232_AbsSerialPort_deviceOpen(JNIEnv *env, jobject from, jstring path,
                                                         jint rate, jint databits, jint stopbits, jint parity)
{
    int fd;
    speed_t speed;
    jobject mFileDescriptor;

    /* 检查波特率 */
    {
        LOGD("baudRate = %d", rate);
        speed = getBaudrate(rate);
        if (speed == -1) {
            LOGE("Invalid baudRate");
            return NULL;
        }
    }

    /*
     * 开启串口
     *
     * //必设 三选一
     * O_RDONLY 只读模式
     * O_WRONLY 只写模式
     * O_RDWR   读写模式
     *
     * //可选
     * O_NOCTTY 如果路径名指向终端设备，不要把这个设备用作控制终端
     * O_NDELAY 表示不关心DCD信号所处的状态
     *
     * PS: 对于串口的打开操作，必须使用O_NOCTTY参数，它表示打开的是一个终端设备，程序不会成为该端口的控制终端。如果不使用此标志，任务的一个输入(比如键盘终止信号等)都会影响进程。
     */
    {
        jboolean iscopy;

        const char *path_utf = (*env)->GetStringUTFChars(env, path, &iscopy);
        LOGD("Opening serial port %s", path_utf);
        fd = open(path_utf, O_RDWR | O_NOCTTY | O_NDELAY | O_CLOEXEC);
        LOGD("open() fd = %d", fd);
        (*env)->ReleaseStringUTFChars(env, path, path_utf);
        if (fd == -1) {
            /* Throw an exception */
            LOGE("Cannot open port");
            return NULL;
        }
        // set_opt(fd, databits, parity, stopbits);
    }

    /* Configure device */
    {
        if (set_opt(fd, speed, databits, parity, stopbits) == -1) {
            return NULL;
        }
    }

    /* Create a corresponding file descriptor */
    {
        jclass cFileDescriptor = (*env)->FindClass(env, "java/io/FileDescriptor");
        jmethodID iFileDescriptor = (*env)->GetMethodID(env, cFileDescriptor, "<init>", "()V");
        jfieldID descriptorID = (*env)->GetFieldID(env, cFileDescriptor, "descriptor", "I");
        mFileDescriptor = (*env)->NewObject(env, cFileDescriptor, iFileDescriptor);
        (*env)->SetIntField(env, mFileDescriptor, descriptorID, (jint)fd);
    }
    return mFileDescriptor;
}

/*
 * Class:     org_bealead_serial_rs232_SerialPort
 * Method:    close
 * Signature: ()V
 */
void
Java_com_seiko_serial_rs232_AbsSerialPort_deviceClose(JNIEnv *env, jobject from)
{

    jclass SerialPortClass = (*env)->GetObjectClass(env, from);
    jclass FileDescriptorClass = (*env)->FindClass(env, "java/io/FileDescriptor");

    jfieldID mFdID = (*env)->GetFieldID(env, SerialPortClass, "mFd", "Ljava/io/FileDescriptor;");
    jfieldID descriptorID = (*env)->GetFieldID(env, FileDescriptorClass, "descriptor", "I");

    jobject mFd = (*env)->GetObjectField(env, from, mFdID);
    jint descriptor = (*env)->GetIntField(env, mFd, descriptorID);

    LOGD("close(fd = %d)", descriptor);
    close(descriptor);
}

/*
 * Class:     org_bealead_serial_rs232_Rs232SerialPort
 * Method:    setBaudRate
 * Signature: ()V
 */
void
Java_com_seiko_serial_rs232_AbsSerialPort_deviceBaudRate(JNIEnv *env, jobject from, jint rate)
{
    int fd;
    speed_t speed;

    /* 检查波特率 */
    {
        LOGD("baudRate = %d", rate);
        speed = getBaudrate(rate);
        if (speed == -1) {
            LOGE("Invalid baudRate");
            return;
        }
    }

    jclass SerialPortClass = (*env)->GetObjectClass(env, from);
    jclass FileDescriptorClass = (*env)->FindClass(env, "java/io/FileDescriptor");

    jfieldID mFdID = (*env)->GetFieldID(env, SerialPortClass, "mFd", "Ljava/io/FileDescriptor;");
    jfieldID descriptorID = (*env)->GetFieldID(env, FileDescriptorClass, "descriptor", "I");

    jobject mFd = (*env)->GetObjectField(env, from, mFdID);
    jint descriptor = (*env)->GetIntField(env, mFd, descriptorID);

    fd = (int) descriptor;

    /* Configure file */
    {
        struct termios newtio, oldtio;

        /*保存测试现有串口参数设置，在这里如果串口号等出错，会有相关的出错信息*/
        if ((tcgetattr(fd, &oldtio)) != 0)
        {
            LOGE("tcgetattr( fd,&oldtio) -> %d\n",tcgetattr( fd,&oldtio));
            return;
        }
        bzero(&newtio, sizeof(newtio));

        /* 设置波特率 */
        cfsetispeed(&newtio, speed);
        cfsetospeed(&newtio, speed);

        /*激活新配置*/
        if((tcsetattr(fd, TCSANOW, &newtio)) != 0)
        {
            LOGW("com set error");
            return;
        }
    }

    LOGD("Set BaudRate = %d", rate);
}
