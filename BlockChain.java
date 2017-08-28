import java.util.ArrayList;
import java.util.HashMap;

// Block Chain should maintain only limited block nodes to satisfy the functions
// You should not have all the blocks added to the block chain in memory 
// as it would cause a memory overflow.

public class BlockChain {
    public static final int CUT_OFF_AGE = 10;
    
    private int maxBlockHeight = 0;
    private byte[] maxHeightBlockHash = null;
    private TransactionPool txPool = new TransactionPool();
    
    private HashMap<byte[], Block> blocks = new HashMap<byte[], Block>();
    private HashMap<byte[], UTXOPool> blockUtxoPoolMap = new HashMap<byte[], UTXOPool>();
    private HashMap<byte[], Integer> blockHeightMap = new HashMap<byte[], Integer>();

    /**
     * create an empty block chain with just a genesis block. Assume {@code genesisBlock} is a valid
     * block
     */
    public BlockChain(Block genesisBlock) {
    	    blocks.put(genesisBlock.getHash(), genesisBlock);

    	    TxHandler txHandler = new TxHandler(new UTXOPool());
    	    
    	    ArrayList<Transaction> txs = genesisBlock.getTransactions();
    	    txHandler.handleTxs(txs.toArray(new Transaction[txs.size()]));
    	    UTXOPool utxoPool = txHandler.getUTXOPool();
    	    addCoinbaseToUtxoPool(genesisBlock.getCoinbase(), utxoPool);
    	    
    	    blockUtxoPoolMap.put(genesisBlock.getHash(), utxoPool);
    	    blockHeightMap.put(genesisBlock.getHash(), 1);
    	    maxBlockHeight = 1;
    	    maxHeightBlockHash = genesisBlock.getHash();
    }
    
    private void addCoinbaseToUtxoPool(Transaction coinbase, UTXOPool utxoPool) {
    	    ArrayList<Transaction.Output> txOutputs = coinbase.getOutputs();
    	    for (int i = 0; i < txOutputs.size(); i++) {
    	    	    utxoPool.addUTXO(new UTXO(coinbase.getHash(), i), txOutputs.get(i));
    	    }
    }

    /** Get the maximum height block */
    public Block getMaxHeightBlock() {
    	    if (blocks.containsKey(maxHeightBlockHash)) {
    	    	    return blocks.get(maxHeightBlockHash);
    	    }
    	    return null;
    }

    /** Get the UTXOPool for mining a new block on top of max height block */
    public UTXOPool getMaxHeightUTXOPool() {
        if (this.blockUtxoPoolMap.containsKey(maxHeightBlockHash)) {
        	    return blockUtxoPoolMap.get(maxHeightBlockHash);
        }
        return null;
    }

    /** Get the transaction pool to mine a new block */
    public TransactionPool getTransactionPool() {
        return txPool;
    }

    /**
     * Add {@code block} to the block chain if it is valid. For validity, all transactions should be
     * valid and block should be at {@code height > (maxHeight - CUT_OFF_AGE)}.
     * 
     * <p>
     * For example, you can try creating a new block over the genesis block (block height 2) if the
     * block chain height is {@code <=
     * CUT_OFF_AGE + 1}. As soon as {@code height > CUT_OFF_AGE + 1}, you cannot create a new block
     * at height 2.
     * 
     * @return true if block is successfully added
     */
    public boolean addBlock(Block block) {
    	    byte[] prevBlockHash = block.getPrevBlockHash();
    	    
    	    // check for Genesis block
        if (prevBlockHash == null) {
        		return false;
        }

        // check for valid coinbase
        Transaction coinbase = block.getCoinbase();
        if (!coinbase.isCoinbase() || coinbase.getOutput(0).value != Block.COINBASE) {
        	    return false;
        }

        // check for valid prevBlockHash pointer
        if (!blocks.containsKey(prevBlockHash)) {
        	    return false;
        }

        // check for valid height
        int prevBlockHeight = blockHeightMap.get(prevBlockHash);
        if (prevBlockHeight < maxBlockHeight - CUT_OFF_AGE) {
        	    return false;
        }
        
        UTXOPool prevBlockUtxoPool = blockUtxoPoolMap.get(prevBlockHash);
        TxHandler txHandler = new TxHandler(prevBlockUtxoPool);
        
        ArrayList<Transaction> txs = block.getTransactions();
        Transaction[] validTxs = txHandler.handleTxs(txs.toArray(new Transaction[txs.size()]));
        if (validTxs.length < txs.size()) {
        	    return false;
        }

        blocks.put(block.getHash(), block);
        blockHeightMap.put(block.getHash(), prevBlockHeight + 1);
        if (prevBlockHeight + 1 > maxBlockHeight) {
        	    maxBlockHeight = prevBlockHeight + 1;
        	    maxHeightBlockHash = block.getHash();
        }
        
        UTXOPool utxoPool = txHandler.getUTXOPool();
        addCoinbaseToUtxoPool(block.getCoinbase(), utxoPool);
        blockUtxoPoolMap.put(block.getHash(), utxoPool);

        txPool = new TransactionPool();
        return true;
    }

    /** Add a transaction to the transaction pool */
    public void addTransaction(Transaction tx) {
    	    txPool.addTransaction(tx);
    }
}