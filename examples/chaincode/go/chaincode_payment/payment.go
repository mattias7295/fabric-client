package main

import (

	"fmt"
	"github.com/hyperledger/fabric/core/chaincode/shim"
	"strconv"

	pb "github.com/hyperledger/fabric/protos/peer"
)


type SimpleChaincode struct {
}

func (t *SimpleChaincode) Init(stub shim.ChaincodeStubInterface) pb.Response {
    var A, B string
    var Aval, Bval, NumAcc int
    var err error
	_, args := stub.GetFunctionAndParameters()
	if len(args) != 5 {
		return shim.Error("Incorrect number of arguments. Expecting 5")
	}

	// Initialize the chaincode
	A = args[0]
	Aval, err = strconv.Atoi(args[1])
	if err != nil {
		return shim.Error("Expecting integer value for asset holding")
	}
	B = args[2]
	Bval, err = strconv.Atoi(args[3])
	if err != nil {
		return shim.Error("Expecting integer value for asset holding")
	}

    NumAcc, err = strconv.Atoi(args[4])
    if err != nil {
        return shim.Error("Expecting integer value for num of accounts")
    }

	fmt.Printf("Aval = %d, Bval = %d, NumAcc = %d\n", Aval, Bval, NumAcc)

    for i := 0; i < NumAcc; i++ {
        // Write the state to the ledger
        err = stub.PutState(A + strconv.Itoa(i), []byte(strconv.Itoa(Aval)))
        if err != nil {
            return shim.Error(err.Error())
        }
        err = stub.PutState(B + strconv.Itoa(i), []byte(strconv.Itoa(Bval)))
        if err != nil {
            return shim.Error(err.Error())
        }
    }

    return shim.Success(nil)

}

func (t *SimpleChaincode) testpay (stub shim.ChaincodeStubInterface, args []string) pb.Response {
    err := stub.PutState(args[1], []byte("1"))
    if err != nil {
        return shim.Error("Error")
    }
    return shim.Success(nil)
}


func (t *SimpleChaincode) pay (stub shim.ChaincodeStubInterface, args []string) pb.Response {
	// Transaction makes payment of X units from A to B
    var A, B string
    var Aval, Bval int
    var X int
    var err error

    if len(args) != 4 {
        return shim.Error("Incorrect number of arguments. Expecting 4")
    }

    A = args[1]
    B = args[2]

    // Get state from the ledger
    Avalbytes, err := stub.GetState(A)
    if err != nil {
        return shim.Error("Failed to get state")
    }
    if Avalbytes == nil {
        return shim.Error("Entity not found")
    }

    Aval, _ = strconv.Atoi(string(Avalbytes))

    Bvalbytes, err := stub.GetState(B)
    if err != nil {
        return shim.Error("Failed to get state")
    }
    if Bvalbytes == nil {
        return shim.Error("Entity not found")
    }

    Bval, _ = strconv.Atoi(string(Bvalbytes))

    X, err = strconv.Atoi(args[3])
    if err != nil {
        return shim.Error("Invalid argument, expecting a integer value")
    }

    Aval = Aval - X
    Bval = Bval + X


    // Write the state to the ledger
    err = stub.PutState(A, []byte(strconv.Itoa(Aval)))
    if err != nil {
        return shim.Error(err.Error())
    }
    stub.PutState(B, []byte(strconv.Itoa(Bval)))
    err = stub.PutState(B, []byte(strconv.Itoa(Bval)))
    if err != nil {
        return shim.Error(err.Error())
    }

    fmt.Printf("Account: %s, Aval = %d. Account: %s, Bval = %d\n", A, Aval, B, Bval)

	return shim.Success(nil)
}

func (t *SimpleChaincode) salary (stub shim.ChaincodeStubInterface, args []string) pb.Response {
    var A string
    var Aval int
    var X int
    var err error

    if len(args) != 3 {
        return shim.Error("Incorrect number of arguments. Expecting 3")
    }

    A = args[1]

    Avalbytes, err := stub.GetState(A)
    if err != nil {
        return shim.Error("Failed to get state")
    }
    if Avalbytes == nil {
        return shim.Error("Entity not found")
    }

    Aval, _ = strconv.Atoi(string(Avalbytes))

    X, err = strconv.Atoi(args[2])
    if err != nil {
        return shim.Error("Invalid argument, expecting a integer value")
    }

    Aval = Aval + X

    err = stub.PutState(A, []byte(strconv.Itoa(Aval)))
    if err != nil {
        return shim.Error(err.Error())
    }

    fmt.Printf("Account: %s, Aval = %d\n", A, Aval)
    return shim.Success(nil)
}

func (t *SimpleChaincode) Invoke(stub shim.ChaincodeStubInterface) pb.Response {
	function, args := stub.GetFunctionAndParameters()

    if function != "invoke" {
        return shim.Error("Unknown function call")
    }

    if len(args) < 2 {
        return shim.Error("Incorrect number of arguments. Expecting at least 2")
    }

	if args[0] == "pay" {
		return t.pay(stub, args)
	}
    if args[0] == "query" {
        return t.query(stub, args)
    }
    if args[0] == "salary" {
        return t.salary(stub, args)
    }
    if args[0] == "testpay" {
        return t.testpay(stub, args)
    }

	return shim.Error("Unknown action, check first argument, must be one of 'pay', 'query', 'salary', 'testpay' function was " + args[0])
}

func (t *SimpleChaincode) query(stub shim.ChaincodeStubInterface, args []string) pb.Response {
    var A string
    var err error

    if len(args) != 2 {
        return shim.Error("Incorrect number of arguments. Expecting 'query', and name of the person to query")
    }

    A = args[1]

    // Get the state from the ledger
    Avalbytes, err := stub.GetState(A)

    if err != nil {
        return shim.Error("Failed to get state for " + A)
    }
    if Avalbytes == nil {
        return shim.Error("Nil amount for " + A)
    }

    return shim.Success(Avalbytes)
}

func main() {
	err := shim.Start(new(SimpleChaincode))
	if err != nil {
		fmt.Printf("Error starting Simple chaincode: %s", err)
	}

}