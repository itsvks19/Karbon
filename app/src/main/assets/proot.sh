PREFIX_PATH=/data/data/com.rk.xededitor
export LD_LIBRARY_PATH=$PREFIX_PATH/root/lib
PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin
$PREFIX_PATH/root/bin/proot -b /sdcard:/sdcard -b /storage:/storage -b $PREFIX_PATH:$PREFIX_PATH --ashmem-memfd --sysvipc -S $PREFIX_PATH/rootfs sh /init.sh