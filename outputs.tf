output "project-variable-product" {
    value = var.project-name
}

output "aws-region-out" {
    value = var.aws-region
}

output "env-out-variable" {
    value=var.envvariable
}

output "VPC-CIDR" {
    value = ["${var.CIDR-Block-VPC}"]
}

output "ssh-port-out" {
    value = "${var.ssh-port}"
}

output "dns-resolution-out" {
    value = "${var.dns-resolution}"
}

output "private-subnet-cndr-out" {
    value = [
        "${var.cidr-private-subnets[0]}",
        "${var.cidr-private-subnets[1]}"
    ]
}

output "private-subnets-cidr-out-mult" {
    value= "${var.cidr-private-subnets[*]}"
}

output "private-subnet-map-out" {
    value = [
        "vpc contains: ${var.private-subnet["vpc"]}",
        "cidr contains: ${var.private-subnet["cidr"]}"
        ]
}

output "private-subnet-map-out-all" {
    value = ["${var.private-subnet[*]}"]
}
