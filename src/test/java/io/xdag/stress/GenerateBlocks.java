package io.xdag.stress;

import io.xdag.config.Config;
import io.xdag.config.DevnetConfig;
import io.xdag.core.Block;
import io.xdag.core.XdagBlock;
import io.xdag.stress.common.ProgressBar;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.security.Security;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.tuweni.bytes.Bytes32;
import org.apache.tuweni.crypto.SECP256K1;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class GenerateBlocks {
    static { Security.addProvider(new BouncyCastleProvider());  }
    private static final String url = "http://127.0.0.1:4444";
    private static BigInteger private_1 = new BigInteger("c85ef7d79691fe79573b1a7064c19c1a9819ebdbd1faaab1a8ec92344438aaf4", 16);
    private static SECP256K1.SecretKey secretkey_1 = SECP256K1.SecretKey.fromInteger(private_1);
    private static final Config config = new DevnetConfig();

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        int num = 200000;
        ProgressBar generateBar = new ProgressBar(num,30);

        List<byte[]> blocks = new ArrayList<>();
        List<Block> blocksOrigin = new ArrayList<>();

        // 1. 创建区块
        System.out.print(new Date(System.currentTimeMillis()));
        System.out.printf(" 创建%d个区块...\n",num);
        for (int i = 0; i < num;i++) {
            SECP256K1.KeyPair addrKey = SECP256K1.KeyPair.fromSecretKey(secretkey_1);
            long xdagTime = System.currentTimeMillis()+i*10;
            Block b = new Block(config, xdagTime, null, null, false, null, null, -1);
            b.signOut(addrKey);
            blocksOrigin.add(b);
            blocks.add(b.getXdagBlock().getData().toArray());
            generateBar.showBarByPoint("创建区块中:",i+1);
        }

        System.out.println();
        System.out.print(new Date(System.currentTimeMillis()));
        System.out.printf(" 创建完成%d个区块\n",num);


        File file = new File("/Users/paulochen/blockdata/block.data");
        if(!file.exists()){
            file.createNewFile();
        }

        FileOutputStream outStream = new FileOutputStream(file);
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(outStream);

        objectOutputStream.writeObject(blocks);
        outStream.close();

        FileInputStream freader = new FileInputStream("/Users/paulochen/blockdata/block.data");

        ObjectInputStream objectInputStream = new ObjectInputStream(freader);

        List<byte[]> blocks1 = new ArrayList<>();
        blocks1 = (List<byte[]>) objectInputStream.readObject();
        System.out.println(blocks1.size());

        List<Block> blocks2 = new ArrayList<>();
        for (byte[] data:blocks1) {
            Block block = new Block(new XdagBlock(data));
            blocks2.add(block);
        }

        for (int i = 0;i<num;i++) {
            if (blocksOrigin.get(i).getHash().compareTo(blocks2.get(i).getHash()) != 0) {
                System.out.println("false");
            }
        }

        System.out.println("true");

//        System.out.println(blocks1.get(0));

    }

}
