import java.util.ArrayList;
import java.util.HashSet;

public class TxHandler {
    
    private UTXOPool utxoPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        this.utxoPool = new UTXOPool(utxoPool);
    }
    
    public UTXOPool getUTXOPool() {
    		return this.utxoPool;
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
    		HashSet<UTXO> claimedUtxoSet = new HashSet<UTXO>();
        ArrayList<Transaction.Output> outputs = tx.getOutputs();
        ArrayList<Transaction.Input> inputs = tx.getInputs();
        
        double outputValueSum = 0, inputValueSum = 0;
        
        for (int i = 0; i < inputs.size(); i++) {
        		Transaction.Input input = inputs.get(i);
        		UTXO prevUtxo = new UTXO(input.prevTxHash, input.outputIndex);

        		if (!utxoPool.contains(prevUtxo)) {
        			return false;
        		}

        		Transaction.Output claimedOutput = utxoPool.getTxOutput(prevUtxo);
        		if (!Crypto.verifySignature(claimedOutput.address, tx.getRawDataToSign(i), input.signature)) {
        			return false;
        		}
        		
        		if (claimedUtxoSet.contains(prevUtxo)) {
        			return false;
        		}
        		claimedUtxoSet.add(prevUtxo);
        		
        		inputValueSum += claimedOutput.value;
        }
        
        for (int i = 0; i < outputs.size(); i++) {
        		Transaction.Output output = outputs.get(i);
        		if (output.value < 0) {
        			return false;
        		}
        		
        		outputValueSum += output.value;
        }
        
        return inputValueSum >= outputValueSum;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        ArrayList<Transaction> validTxs = new ArrayList<Transaction>();
        for (Transaction tx : possibleTxs) {
        		if (!isValidTx(tx)) {
        			continue;
        		}
        		
        		ArrayList<Transaction.Output> txOutputs = tx.getOutputs();
        		for (int i = 0 ; i < txOutputs.size(); i++) {
        			UTXO outputUtxo = new UTXO(tx.getHash(), i);
        			utxoPool.addUTXO(outputUtxo, txOutputs.get(i));
        		}
        		
        		ArrayList<Transaction.Input> txInputs = tx.getInputs();
        		for (int i = 0 ; i < txInputs.size(); i++) {
        			Transaction.Input input = txInputs.get(i);
            		UTXO prevUtxo = new UTXO(input.prevTxHash, input.outputIndex);
            		utxoPool.removeUTXO(prevUtxo);
        		}
        		
        		validTxs.add(tx);
        }
        
        return validTxs.toArray(new Transaction[validTxs.size()]);
    }

}
