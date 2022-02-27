package io.xdag.stress;

import io.xdag.BlockBuilder;
import io.xdag.Kernel;
import io.xdag.config.Config;
import io.xdag.config.DevnetConfig;
import io.xdag.core.Block;
import io.xdag.core.XdagBlock;
import io.xdag.crypto.SampleKeys;
import io.xdag.crypto.jni.Native;
import io.xdag.db.DatabaseFactory;
import io.xdag.db.DatabaseName;
import io.xdag.db.rocksdb.RocksdbFactory;
import io.xdag.db.store.BlockStore;
import io.xdag.db.store.OrphanPool;
import io.xdag.stress.common.ProgressBar;
import io.xdag.wallet.Wallet;
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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.tuweni.bytes.Bytes32;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.hyperledger.besu.crypto.SECP256K1;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class GenerateBlocks {
    @Rule
    public TemporaryFolder root = new TemporaryFolder();
    Config config = new DevnetConfig();
    Wallet wallet;
    String pwd;
    Kernel kernel;
    DatabaseFactory dbFactory;

    long expectedExtraBlocks = 12;

    BigInteger private_1 = new BigInteger("c85ef7d79691fe79573b1a7064c19c1a9819ebdbd1faaab1a8ec92344438aaf4", 16);
    BigInteger private_2 = new BigInteger("10a55f0c18c46873ddbf9f15eddfc06f10953c601fd144474131199e04148046", 16);

    org.hyperledger.besu.crypto.SECP256K1.PrivateKey secretkey_1 = org.hyperledger.besu.crypto.SECP256K1.PrivateKey.create(private_1);
    org.hyperledger.besu.crypto.SECP256K1.PrivateKey secretkey_2 = org.hyperledger.besu.crypto.SECP256K1.PrivateKey.create(private_2);

    @Before
    public void setUp() throws Exception {
        config.getNodeSpec().setStoreDir(root.newFolder().getAbsolutePath());
        config.getNodeSpec().setStoreBackupDir(root.newFolder().getAbsolutePath());

        Native.init(config);
        if (Native.dnet_crypt_init() < 0) {
            throw new Exception("dnet crypt init failed");
        }
        pwd = "password";
        wallet = new Wallet(config);
        wallet.unlock(pwd);
        org.hyperledger.besu.crypto.SECP256K1.KeyPair key = org.hyperledger.besu.crypto.SECP256K1.KeyPair.create(
                SampleKeys.SRIVATE_KEY);
        wallet.setAccounts(Collections.singletonList(key));
        wallet.flush();

        kernel = new Kernel(config);
        dbFactory = new RocksdbFactory(config);

        BlockStore blockStore = new BlockStore(
                dbFactory.getDB(DatabaseName.INDEX),
                dbFactory.getDB(DatabaseName.TIME),
                dbFactory.getDB(DatabaseName.BLOCK));

        blockStore.reset();
        OrphanPool orphanPool = new OrphanPool(dbFactory.getDB(DatabaseName.ORPHANIND));
        orphanPool.reset();

        kernel.setBlockStore(blockStore);
        kernel.setOrphanPool(orphanPool);
        kernel.setWallet(wallet);
    }


    @Test
    public void generateBlocksData() throws IOException, ClassNotFoundException {
        int num = 200000;

        ProgressBar generateBar = new ProgressBar(num,30);

        List<byte[]> blocks = new ArrayList<>();

        // 1. 创建区块
        System.out.print(new Date(System.currentTimeMillis()));
        System.out.printf(" 创建%d个区块...\n",num);
        for (int i = 0; i < num;i++) {
            SECP256K1.KeyPair addrKey = SECP256K1.KeyPair.create(secretkey_1);
            long xdagTime = System.currentTimeMillis()+i*10;
            Block b = BlockBuilder.generateAddressBlock(config,addrKey,xdagTime);
            blocks.add(b.getXdagBlock().getData().toArray());
            generateBar.showBarByPoint("创建区块中:",i+1);
        }

        System.out.println();
        System.out.print(new Date(System.currentTimeMillis()));
        System.out.printf(" 创建完成%d个区块\n",num);


        File file = new File("block.data");
        if(!file.exists()){
            file.createNewFile();
        }

        FileOutputStream outStream = new FileOutputStream(file);
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(outStream);

        objectOutputStream.writeObject(blocks);
        outStream.close();

    }

}
